package cl.duoc.inscripciones.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class AwsS3Service {

    private static final Logger logger = LoggerFactory.getLogger(AwsS3Service.class);
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public AwsS3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String subirGuiaAS3(String rutaEfs, String transportista, String numeroGuia) {
        File archivo = new File(rutaEfs);
        if (!archivo.exists()) {
            throw new RuntimeException("Archivo no encontrado en: " + rutaEfs);
        }

        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = fecha + "/" + transportista.replaceAll("\\s+", "").toLowerCase() + "/guia_" + numeroGuia + ".pdf";

        s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/pdf")
                .build(), RequestBody.fromFile(archivo));
        
        return key;
    }

    public ResponseBytes<GetObjectResponse> descargarGuiaDeS3(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
                
        return s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes());
    }

    public void borrarArchivo(Long id) {
        // Implementación futura si es necesaria
    }
}