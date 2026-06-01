package cl.duoc.inscripciones.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.charset.StandardCharsets;

@Service
public class AwsS3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public AwsS3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // 1. Generar contenido del archivo de texto en caliente
    public String generarContenidoTexto(Long id, String estudiante, String cursos, Double total) {
        return "========================================\n" +
               "       RESUMEN DE INSCRIPCIÓN N° " + id + "\n" +
               "========================================\n" +
               "Estudiante: " + estudiante + "\n" +
               "Cursos Seleccionados:\n" + cursos + "\n" +
               "----------------------------------------\n" +
               "TOTAL A PAGAR: $" + total + "\n" +
               "========================================\n";
    }

    // 2. Subir o Modificar archivo en S3 (Estructura dinámica: bucket/id/resumen.txt)
    public void subirArchivo(Long id, String contenido) {
        String key = id + "/resumen_inscripcion.txt";
        
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("text/plain")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromString(contenido, StandardCharsets.UTF_8));
    }

    // 3. Descargar archivo desde S3
    public String descargarArchivo(Long id) {
        String key = id + "/resumen_inscripcion.txt";
        
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
        return objectBytes.asString(StandardCharsets.UTF_8);
    }

    // 4. Borrar archivo de S3
    public void borrarArchivo(Long id) {
        String key = id + "/resumen_inscripcion.txt";
        
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }
}