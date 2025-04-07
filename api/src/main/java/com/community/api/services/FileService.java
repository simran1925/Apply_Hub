package com.community.api.services;

import com.community.api.services.exception.ExceptionHandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
@Service
public class FileService {



    @Autowired
    ExceptionHandlingService exceptionHandling;

    @Value("${file.server.url}")
    private String fileServerUrl;

    /**
     * Generates a public URL for the file.
     *
     * @param filePath The relative path to the file.
     * @return The URL to access the file.
     */


    public String getFileUrl(String filePath, HttpServletRequest request) {
        try{
            String normalizedFilePath = filePath.replace("\\", "/");

//            return   this.getFileUrl(normalizedFilePath);
      return fileServerUrl + "/"  + normalizedFilePath;
        }catch (Exception e){
            exceptionHandling.handleException(e);
            return "Error fetching urls:  " + e.getMessage();
        }
    }

    public String getDownloadFileUrl(String filePath, HttpServletRequest request) {
        String normalizedFilePath = filePath.replace("\\", "/");

        String[] pathSegments = normalizedFilePath.split("/");
        StringBuilder encodedFilePath = new StringBuilder();

        for (String segment : pathSegments) {
            if (encodedFilePath.length() > 0) {
                encodedFilePath.append("/");
            }
            String encodedSegment = URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
            encodedFilePath.append(encodedSegment);
        }

        return fileServerUrl + "/" + encodedFilePath.toString();
    }
    public String getFileUrl(String fullFilePath) {
        try {
            String formattedPath = fullFilePath.replace("\\", "/");
            String encodedPath = URLEncoder.encode(formattedPath, StandardCharsets.UTF_8.toString());

            String fileUrlApi = fileServerUrl + "/files/file-url?filePath=" + encodedPath;


            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.add("ngrok-skip-browser-warning", "true");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(fileUrlApi, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                return "Error fetching URL: " + response.getStatusCode() + " - " + response.getBody();
            }
        } catch (Exception e) {
            exceptionHandling.handleException(e);
            return "Error fetching URLs: " + e.getMessage();
        }
    }
    // public String getFileUrl(String fullFilePath) {
    //     try {
    //         String formattedPath = fullFilePath.replace("\\", "/");

    //         String encodedPath = URLEncoder.encode(formattedPath, StandardCharsets.UTF_8.toString());

    //         String fileUrlApi = fileServerUrl + "/files/file-url?filePath=" + encodedPath;

    //         System.out.println("Calling API: " + fileUrlApi);

    //         RestTemplate restTemplate = new RestTemplate();
    //         String fileUrl = restTemplate.getForObject(fileUrlApi, String.class);
    //         return fileUrl;
    //     } catch (Exception e) {
    //         exceptionHandling.handleException(e);
    //         return "Error fetching URLs: " + e.getMessage();
    //     }

    // }


}