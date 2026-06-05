package cl.duoc.inscripciones.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class AwsS3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public AwsS3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // ===================================================================
    // MÉTODOS NUEVOS - SEMANA 3 (GUÍAS DE DESPACHO)
    // ===================================================================
    public String subirGuiaAS3(String rutaEfs, String transportista, String numeroGuia) {
        File archivo = new File(rutaEfs);
        if (!archivo.exists()) {
            throw new RuntimeException("El archivo temporal en el EFS no existe: " + rutaEfs);
        }

        String fechaFormateada = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String transportistaClean = transportista.replaceAll("\\s+", "").toLowerCase();
        String s3Key = fechaFormateada + "/" + transportistaClean + "/guia_" + numeroGuia + ".pdf";

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType("application/pdf")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(archivo));
        return s3Key;
    }

    public void eliminarGuiaDeS3(String s3Key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    // ===================================================================
    // MÉTODOS ANTIGUOS RESPALDADOS - SEMANA 2 (INSCRIPCIONES)
    // ===================================================================
    public String generarContenidoTexto(Long id, String alumno, String curso, Double total) {
        return "========================================\n" +
               "RESUMEN DE INSCRIPCIÓN N° " + id + "\n" +
               "========================================\n" +
               "Estudiante: " + alumno + "\n" +
               "Cursos Seleccionados:\n" +
               "- " + curso + " ($" + total + ")\n" +
               "----------------------------------------\n" +
               "TOTAL A PAGAR: $" + total + "\n" +
               "========================================\n";
    }

    public void subirArchivo(Long id, String contenido) {
        String s3Key = id + "/resumen.txt";
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType("text/plain")
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromString(contenido, StandardCharsets.UTF_8));
    }

    public String descargarArchivo(Long id) {
        String s3Key = id + "/resumen.txt";
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
        return objectBytes.asString(StandardCharsets.UTF_8);
    }

    public void borrarArchivo(Long id) {
        String s3Key = id + "/resumen.txt";
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }
}