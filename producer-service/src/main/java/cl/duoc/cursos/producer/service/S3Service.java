package cl.duoc.cursos.producer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.io.File;
import java.net.URLConnection;

@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String awsS3BucketName;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // Recibe la llave pre-armada desde el service
    public String subirArchivoConKey(String s3Key, File archivo) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsS3BucketName)
                .key(s3Key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(archivo));
        return s3Key;
    }

    // Detección del Content-Type para aceptar Word, PPT, imágenes o PDFs
    public String subirArchivoBytes(String s3Key, byte[] archivoBytes) {
        String contentType = URLConnection.guessContentTypeFromName(s3Key);
        if (contentType == null) {
            contentType = "application/octet-stream"; 
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsS3BucketName)
                .key(s3Key)
                .contentType(contentType) 
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(archivoBytes));
        return s3Key;
    }

    // Modificar y actualizar los archivos en AWS S3
    public void actualizarArchivo(String s3Key, File nuevoArchivo) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsS3BucketName)
                .key(s3Key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(nuevoArchivo));
    }

    // Descargar los archivos desde AWS S3
    public byte[] descargarArchivo(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(awsS3BucketName)
                .key(s3Key)
                .build();
                
        return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
    }

    // Eliminar un archivo de S3 automáticamente cuando se borre en el sistema
    public void eliminarArchivo(String s3Key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(awsS3BucketName)
                .key(s3Key)
                .build();
                
        s3Client.deleteObject(deleteObjectRequest);
    }
}