import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Service
public class AwsS3Service {

    private final S3Client s3Client;
    private final String bucketName = "duoc-inscripciones-bucket-cristian-olivares";

    public AwsS3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String subirGuiaAS3(InputStream inputStream, long contentLength, String transportista, String numeroGuia) {
        String key = "guias/" + transportista.replaceAll("\\s+", "").toLowerCase() + "/guia_" + numeroGuia + ".pdf";
        
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/pdf")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
        
        return key; // Retorna la key para guardarla en la BD
    }
}