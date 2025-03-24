    package com.research.assistant;


    import com.fasterxml.jackson.databind.ObjectMapper;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Service;
    import org.springframework.web.reactive.function.client.WebClient;

    import java.util.Map;

    @Service
    public class ResearchService {

        @Value("${gemini.api.url}")
        private String geminiApiUrl;

        @Value("${gemini.api.key}")
        private String geminiApiKey;

        private final WebClient webClient;
        private final ObjectMapper objectMapper;

        public ResearchService(WebClient.Builder webClientBuilder,ObjectMapper objectMapper) {
            this.webClient = webClientBuilder.build();
            this.objectMapper = objectMapper;

        }


        public String processContent(ResearchRequest request) {
            // Build prompt
            String prompt = buildPrompt(request);
            // Query the api
            Map<String,Object> requestBody = Map.of(
              "contents",new Object[]{
                      Map.of("parts",new Object[]{
                              Map.of("text",prompt)
                      })
                    }
            );
            String response = webClient.post()
                    .uri(geminiApiUrl+geminiApiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            // parse the response





            /// return response
            return extractTextFromResponse(response);
        }

        private String extractTextFromResponse(String response) {
           try{
                  GeminiResponse geminiResponse = objectMapper.readValue(response,GeminiResponse.class);
                  if (geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) {
                      GeminiResponse.Candidate firstCandidate = geminiResponse.getCandidates().get(0);
                      if (firstCandidate.getContent() != null && firstCandidate.getContent().getParts()!=null && !firstCandidate.getContent().getParts().isEmpty()) {
                                return firstCandidate.getContent().getParts().get(0).getText();
                      }
                  }
                  return "No content Found In Response Check It ?? ";
           }
           catch(Exception e){
               return "Error Parsing Response" + e.getMessage();
           }
        }

        private String buildPrompt(ResearchRequest request) {
            StringBuilder prompt = new StringBuilder();
            switch(request.getOperation()) {
                case "summarize":
                    prompt.append("Provide a comprehensive bulleted summary that captures all key information from this content in a well-structured format. in order list manner");
                    break;
                case "suggest":
                    prompt.append("Based on this content, recommend related topics and further reading. Format with clear headings and concise bullet points. in order list manner ");
                    break;
                case "timeComplexity":
                    prompt.append("Please tell time and space complexity in very clean way: give reason also in short");
                    break;
                case "similarQuestion":
                    prompt.append("GIve only questions similar to this DSA question with link of question related to that topics");
                    break;
                case "simplify":
                    prompt.append("See this page and generate test cases related to this questions with good accuracy generate test cases for this question");
                    break;
                case "keyInsights":
                    prompt.append("Extract the 3-5 most important insights from this content, explaining why each matters and its practical implications.");
                    break;
                case "actionItems":
                    prompt.append("Convert this information into a practical checklist of specific, actionable steps a user could take.");
                    break;
                case "visualize":
                    prompt.append("Describe how this information could be effectively represented visually (chart, diagram, infographic), including what elements to include and why.");
                    break;
                default:
                    throw new IllegalArgumentException("Unknown operation: " + request.getOperation());
            }
            prompt.append(request.getContent());
            return prompt.toString();
        }
    }
