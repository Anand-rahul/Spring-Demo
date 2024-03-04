package com.example.Interviewer;

import com.example.Interviewer.Model.ChatGPT.ChatRequest;
import com.example.Interviewer.Model.ChatGPT.ChatResponse;
import com.example.Interviewer.Model.DbClass;
import com.example.Interviewer.Model.DbClassRepo;
import com.example.Interviewer.Model.GPTModel;
import com.example.Interviewer.Model.GPTRepo;
import jakarta.validation.Valid;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class Controller {

    @Autowired
    private DbClassRepo repoInst;
    @Autowired
    private GPTRepo ChatGPTRepoInstance;
    @GetMapping("/Questions")
    public List<DbClass> getQuestions(){
        return repoInst.findAll();
    }

    @GetMapping("/Questions/{id}")
    public ResponseEntity<String> getQuestionById(
            @PathVariable(value = "id") Long Id
    )throws ResourceNotFoundException{
        DbClass instance=repoInst.findById(Id).orElseThrow(()->new ResourceNotFoundException("Question Number Not Found::"+Id));
        return ResponseEntity.ok().body(instance.getQuestion());
    }

    @PostMapping("/Questions")
    public DbClass createQuestion(@Valid @RequestBody DbClass instance){
        return repoInst.save(instance);
    }

    @PutMapping("/Questions/{id}")
    public ResponseEntity<DbClass> updateQuestion(@PathVariable(value="id") Long Id,@Valid @RequestBody DbClass instance) throws ResourceNotFoundException{
        DbClass newVar=repoInst.findById(Id).orElseThrow(()->new ResourceNotFoundException("Question Not Found::"+Id));
        newVar.setQuestion(instance.getQuestion());
        final DbClass updatedInstance=repoInst.save(newVar);
        return ResponseEntity.ok(updatedInstance);
    }

    @DeleteMapping("/Questions/{id}")
    public Map<String,Boolean> deleteQuestion(@PathVariable(value="id")Long Id)throws ResourceNotFoundException{
        DbClass newInstance=repoInst.findById(Id).orElseThrow(()->new ResourceNotFoundException("Question not Found::"+Id));
        repoInst.delete(newInstance);
        var response=new HashMap<String,Boolean>();
        response.put("Deleted",Boolean.TRUE);
        return response;
    }
    @GetMapping("/Questions/Prompt/{id}")
    public ResponseEntity<GPTModel> getQuestionByIdForGPT(
            @PathVariable(value = "id") Long Id
    )throws ResourceNotFoundException{
        DbClass instance=repoInst.findById(Id).orElseThrow(()->new ResourceNotFoundException("Question Number Not Found::"+Id));
        GPTModel newChatModel= new GPTModel();
        newChatModel.setQuestion(instance.getQuestion());
        String prompt=chat(newChatModel.getQuestion());
        newChatModel.setPrompt(prompt);
        ChatGPTRepoInstance.save(newChatModel);
        return ResponseEntity.ok().body(newChatModel);
    }

    @Qualifier("openaiRestTemplate")
    @Autowired
    private RestTemplate restTemplate;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiUrl;

    public String chat(String prompt) {
        // create a request
        ChatRequest request = new ChatRequest(model, prompt);

        // call the API
        ChatResponse response = restTemplate.postForObject(apiUrl, request, ChatResponse.class);

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return "No response";
        }
        System.out.println(response.getChoices().getFirst().getMessage().getContent());
        return response.getChoices().getFirst().getMessage().getContent();
    }
}
