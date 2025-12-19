package com.togettoyou.fabricrealty.springbootserver.fabric;

import com.togettoyou.fabricrealty.springbootserver.config.FabricProperties;
import com.togettoyou.fabricrealty.springbootserver.config.OrganizationProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class FabricGatewayClient implements FabricClient, FabricNetworkProvider, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(FabricGatewayClient.class);

    private final FabricProperties fabricProperties;

    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();
    private final Map<String, Gateway> gateways = new ConcurrentHashMap<>();
    private final Map<String, Network> networks = new ConcurrentHashMap<>();
    private final Map<String, Contract> contracts = new ConcurrentHashMap<>();

    public FabricGatewayClient(FabricProperties fabricProperties) {
        this.fabricProperties = fabricProperties;
    }

    @PostConstruct
    public void init() {
        if (fabricProperties.organizations() == null || fabricProperties.organizations().isEmpty()) {
            throw new IllegalStateException("fabric.organizations 未配置，无法初始化 Fabric Gateway");
        }
        requireNonBlank(fabricProperties.channelName(), "fabric.channelName");
        requireNonBlank(fabricProperties.chaincodeName(), "fabric.chaincodeName");

        for (Map.Entry<String, OrganizationProperties> entry : fabricProperties.organizations().entrySet()) {
            String orgName = entry.getKey();
            OrganizationProperties org = entry.getValue();
            try {
                ManagedChannel channel = newGrpcConnection(org);
                Identity identity = newIdentity(org);
                Signer signer = newSigner(org);

                Gateway gateway = Gateway.newInstance()
                        .identity(identity)
                        .signer(signer)
                        .connection(channel)
                        .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                        .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                        .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                        .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES))
                        .connect();

                Network network = gateway.getNetwork(fabricProperties.channelName());
                Contract contract = network.getContract(fabricProperties.chaincodeName());

                channels.put(orgName, channel);
                gateways.put(orgName, gateway);
                networks.put(orgName, network);
                contracts.put(orgName, contract);

                log.info("Fabric gateway initialized: org={}, peerEndpoint={}, gatewayPeer={}, mspId={}",
                        orgName, org.peerEndpoint(), org.gatewayPeer(), org.mspId());
            } catch (Exception e) {
                closeQuietly();
                throw new IllegalStateException("初始化组织[" + orgName + "]的 Fabric Gateway 失败: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public byte[] evaluate(String orgName, String transactionName, String... args) {
        Contract contract = getContract(orgName);
        try {
            return contract.evaluateTransaction(transactionName, args);
        } catch (Exception e) {
            throw new RuntimeException(FabricErrorTranslator.toUserMessage(e), e);
        }
    }

    @Override
    public byte[] submit(String orgName, String transactionName, String... args) {
        Contract contract = getContract(orgName);
        try {
            return contract.submitTransaction(transactionName, args);
        } catch (Exception e) {
            throw new RuntimeException(FabricErrorTranslator.toUserMessage(e), e);
        }
    }

    private Contract getContract(String orgName) {
        Contract contract = contracts.get(orgName);
        if (contract == null) {
            throw new IllegalArgumentException("未知组织: " + orgName);
        }
        return contract;
    }

    @Override
    public Network getNetwork(String orgName) {
        Network network = networks.get(orgName);
        if (network == null) {
            throw new IllegalArgumentException("未知组织: " + orgName);
        }
        return network;
    }

    private static ManagedChannel newGrpcConnection(OrganizationProperties org) throws IOException, CertificateException {
        requireNonBlank(org.peerEndpoint(), "peerEndpoint");
        requireNonBlank(org.gatewayPeer(), "gatewayPeer");
        requireNonBlank(org.tlsCertPath(), "tlsCertPath");

        var credentials = TlsChannelCredentials.newBuilder()
                .trustManager(Path.of(org.tlsCertPath()).toFile())
                .build();

        return Grpc.newChannelBuilder(org.peerEndpoint(), credentials)
                .overrideAuthority(org.gatewayPeer())
                .build();
    }

    private static Identity newIdentity(OrganizationProperties org) throws IOException, CertificateException {
        requireNonBlank(org.mspId(), "mspId");
        requireNonBlank(org.certPath(), "certPath");

        Path certificatePath = firstFile(Path.of(org.certPath()));
        X509Certificate certificate = readX509Certificate(certificatePath);
        log.debug("Loaded identity certificate: subject={}", safeSubject(certificate));
        return new X509Identity(org.mspId(), certificate);
    }

    private static Signer newSigner(OrganizationProperties org) throws IOException, InvalidKeyException {
        requireNonBlank(org.keyPath(), "keyPath");

        Path privateKeyPath = firstFile(Path.of(org.keyPath()));
        PrivateKey privateKey = readPrivateKey(privateKeyPath);
        return Signers.newPrivateKeySigner(privateKey);
    }

    private static X509Certificate readX509Certificate(Path path) throws IOException, CertificateException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return Identities.readX509Certificate(reader);
        }
    }

    private static PrivateKey readPrivateKey(Path path) throws IOException, InvalidKeyException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return Identities.readPrivateKey(reader);
        }
    }

    private static Path firstFile(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            throw new IOException("目录不存在: " + directory);
        }
        try (var stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::getFileName))
                    .findFirst()
                    .orElseThrow(() -> new IOException("目录下未找到文件: " + directory));
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("配置缺失: " + fieldName);
        }
    }

    private static String safeSubject(X509Certificate certificate) {
        try {
            X500Principal subject = certificate.getSubjectX500Principal();
            return subject == null ? "" : Objects.toString(subject.getName(), "");
        } catch (Exception ignored) {
            return "";
        }
    }

    @Override
    public void close() {
        closeQuietly();
    }

    @PreDestroy
    public void shutdown() {
        closeQuietly();
    }

    private void closeQuietly() {
        for (Gateway gateway : gateways.values()) {
            try {
                gateway.close();
            } catch (Exception ignored) {
            }
        }
        gateways.clear();
        networks.clear();

        for (ManagedChannel channel : channels.values()) {
            try {
                channel.shutdownNow();
            } catch (Exception ignored) {
            }
        }
        channels.clear();
        contracts.clear();
    }
}
