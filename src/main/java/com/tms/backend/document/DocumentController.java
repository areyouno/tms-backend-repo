package com.tms.backend.document;
// package com.tms.demoservice.document;

// import java.util.List;
// import java.util.Map;

// import org.springframework.web.bind.annotation.CrossOrigin;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PutMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import lombok.RequiredArgsConstructor;

// @RestController
// @RequestMapping("/api/docs")
// @RequiredArgsConstructor
// @CrossOrigin(origins = "http://localhost:5173") // Vite default
// public class DocumentController {
//   private final DocumentService service;

//   /** GET /api/docs/{id}/lines -> ["Line 1", "Line 2", …] */
//   @GetMapping("/{id}/lines")
//   public List<String> lines(@PathVariable Long id) { return service.getLines(id); }

//   /** PUT /api/docs/{id}/lines/{n} -> JSON body: {"text":"new content"} */
//   @PutMapping("/{id}/lines/{n}")
//   public void updateLine(@PathVariable Long id,
//                          @PathVariable int n,
//                          @RequestBody Map<String,String> body) {
//     service.updateLine(id, n, body.getOrDefault("text", ""));
//   }
// }

