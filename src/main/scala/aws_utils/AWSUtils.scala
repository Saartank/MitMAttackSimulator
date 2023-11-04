package aws_utils

import org.slf4j.LoggerFactory
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest

import java.io.{InputStream, ObjectInputStream}
import java.io.ByteArrayInputStream
import scala.io.Source
import java.net.URI
import scala.io.BufferedSource
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.apache.ApacheHttpClient

import java.io.ByteArrayOutputStream
import java.io.ByteArrayOutputStream

/**
 * This object includes functions to read and write objects to S3.
 */

object AWSUtils {
  private val logger = LoggerFactory.getLogger(getClass.getSimpleName)

  def getS3File(s3Path: String, region: Region = Region.US_EAST_2): String = {
    logger.info(s"ApacheHttpClient.CLIENT_NAME: ${ApacheHttpClient.CLIENT_NAME}")
    logger.info(s"Getting file: $s3Path")
    val uri = new URI(s3Path)
    val bucket = uri.getHost
    logger.info(s"Using bucket: $bucket")
    val key = uri.getPath.drop(1) // Drop the leading slash
    logger.info(s"Using key: $key")

    val s3Client = S3Client.builder.region(region).httpClientBuilder(ApacheHttpClient.builder()).build()
    logger.info(s"Created S3 client...")

    val request = GetObjectRequest.builder.bucket(bucket).key(key).build()
    logger.info(s"Created request...")

    val s3Object = s3Client.getObject(request)
    logger.info(s"Fetched S3 Object...")

    Source.fromInputStream(s3Object).mkString
  }

  def writeS3string(data: String, s3Path: String, region: Region = Region.US_EAST_2): Unit = {
    logger.info(s"ApacheHttpClient.CLIENT_NAME: ${ApacheHttpClient.CLIENT_NAME}")
    logger.info(s"Writing string in file: $s3Path")
    val uri = new URI(s3Path)
    val bucket = uri.getHost
    logger.info(s"Using bucket: $bucket")
    val key = uri.getPath.drop(1) // Drop the leading slash
    logger.info(s"Using key: $key")

    val s3Client = S3Client.builder.region(region).httpClientBuilder(ApacheHttpClient.builder()).build()
    logger.info(s"Created S3 client...")

    s3Client.putObject(
      PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build(),
      RequestBody.fromString(data)
    )
    logger.info(s"Successfully written string in file : $s3Path")
    s3Client.close()

  }
}
