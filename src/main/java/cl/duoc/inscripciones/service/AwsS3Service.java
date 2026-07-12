package cl.duoc.inscripciones.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.ResponseBytes;

import java.io.File;
import java.nio.charset.StandardCharsets;
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

    // ===================================================================
    // MÉTODOS PARA GUÍAS DE DESPACHO
    // ===================================================================
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

    // ===================================================================
    // MÉTODOS PARA INSCRIPCIONES
    // ===================================================================
    public String generarContenidoTexto(Long id, String alumno, String curso, Double total) {
        return "RESUMEN DE INSCRIPCIÓN N° " + id + "\n" +
               "Estudiante: " + alumno + "\n" +
               "Cursos: " + curso + "\n" +
               "TOTAL: $" + total + "\n";
    }

    public void subirArchivo(Long id, String contenido) {
        String s3Key = id + "/resumen.txt";
        logger.info("Intentando subir archivo a S3 bucket: {} con clave: {}", bucketName, s3Key);
        
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType("text/plain")
                    .build(), RequestBody.fromString(contenido, StandardCharsets.UTF_8));
        } catch (S3Exception e) {
            logger.error("Error de S3 al subir archivo: {}", e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    public String descargarArchivo(Long id) {
        String s3Key = id + "/resumen.txt";
        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build());
        return objectBytes.asString(StandardCharsets.UTF_8);
    }

    public void borrarArchivo(Long id) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(id + "/resumen.txt")
                .build());
    }
}