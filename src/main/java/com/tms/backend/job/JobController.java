package com.tms.backend.job;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.tms.backend.dto.FileDownloadDTO;
import com.tms.backend.dto.JobDTO;
import com.tms.backend.dto.JobEditDTO;
import com.tms.backend.exception.ResourceNotFoundException;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


// @RestController
@RequestMapping("/job")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService){
        this.jobService = jobService;
    }
    
    @PostMapping("/upload")
    public ResponseEntity<JobDTO> uploadFile(@RequestParam("file") MultipartFile file,
    @RequestParam("status") String status,
    @RequestParam("targetLang") Set<String> targetLang,
    @RequestParam("provider") String provider,
    @RequestParam("dueDate") LocalDateTime dueDate,
    @RequestParam("projectId") Long projectId
    )
    {
        try {
            Job savedJob = jobService.saveFile(file, status, targetLang, provider, dueDate, projectId);
            //store to db or filesystem
            return ResponseEntity.ok(jobService.convertToDTO(savedJob));
        } 
        catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(null);
        }
    }

    @Value("${file.upload-dir}")
    private String baseUploadDir;

    public Path getUserUploadPath(Long userId) {
        return Paths.get(baseUploadDir, String.valueOf(userId));
    }

    @PostMapping("/upload2")
    public ResponseEntity<String> uploadFile2(
        @RequestParam("file") MultipartFile file) { //, @AuthenticationPrincipal CustomUserDetails currentUser
            
        // Long userId = currentUser.getId();
            try {
                jobService.saveFile2(file);
                return ResponseEntity.ok("File uploaded successfully");
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error uploading file: " + e.getMessage());
            }
        // try {
        //     // ensure directory exists
        //     Path dirPath = getUserUploadPath(userId);
        //     Files.createDirectories(dirPath);

        //     // save file
        //     Path filePath = dirPath.resolve(file.getOriginalFilename());
        //     Files.write(filePath, file.getBytes());

        //     return ResponseEntity.ok("File uploaded successfully: " + filePath);
        // }
        // catch(IOException e) {
        //     return ResponseEntity.status(500).body("File upload failed: " + e.getMessage());
        // }
        
    }
    

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadFileById(
        @PathVariable Long id,
        @RequestParam Long currentUserId
        ){
            try {
                FileDownloadDTO fileDownloadDTO = jobService.downloadJobFile(id, currentUserId);

                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"" + fileDownloadDTO.filename() + "\"")
                        .contentType(MediaType.parseMediaType(fileDownloadDTO.contentType()))
                        .body(fileDownloadDTO.data());

            } catch (ResourceNotFoundException e) {
                return ResponseEntity.notFound().build();
            } catch (AccessDeniedException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobEditDTO> update(@PathVariable Long id, @RequestBody JobEditDTO dto) {
        JobEditDTO updated = jobService.update(id, dto);
        
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteJob(@PathVariable Long id){
        jobService.deleteJob(id);
        return ResponseEntity.ok("Job deleted successfully");
    }
}
