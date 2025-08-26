package com.tms.backend.document;
// package com.tms.demoservice.document;

// import java.util.ArrayList;
// import java.util.List;

// import org.springframework.stereotype.Service;
// import org.springframework.web.server.ResponseStatusException;
// import org.springframework.http.HttpStatus;  
// import lombok.RequiredArgsConstructor;

// @Service
// @RequiredArgsConstructor
// public class DocumentService {
//   private final DocumentRepository repo;

//   public List<String> getLines(Long id) {
//     return repo.findById(id)
//                .map(doc -> List.of(doc.getContent().split("\\R"))) // \R = any newline
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//   }

//   public void updateLine(Long id, int line, String newText) {
//     Document doc = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//     List<String> lines = new ArrayList(List.of(doc.getContent().split("\\R", -1)));
//     if (line < 0 || line >= lines.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
//     lines.set(line, newText);
//     doc.setContent(String.join("\n", lines));
//     repo.save(doc);
//   }
  
// }