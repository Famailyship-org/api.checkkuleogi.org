package com.Familyship.checkkuleogi.domains.book.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.Familyship.checkkuleogi.domains.book.domain.Book;
import com.Familyship.checkkuleogi.domains.book.domain.BookMBTI;
import com.Familyship.checkkuleogi.domains.book.domain.repository.BookRepository;
import com.Familyship.checkkuleogi.domains.book.dto.BookMBTIRequest;
import com.Familyship.checkkuleogi.domains.book.dto.BookMBTIResponse;
import com.Familyship.checkkuleogi.domains.book.dto.ChatGPTRequest;
import com.Familyship.checkkuleogi.domains.book.dto.ChatGPTResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;


@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private RestTemplate template;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiURL;

    @Autowired
    private BookRepository bookRepository;


    @Override
    public BookMBTIResponse createBook(BookMBTIRequest request) {

        // 각 프롬프트에 대한 ChatGPT API 호출
        String mbtiContent = callChatGPT(promptChatGPT(request, "EI")) + "\n"
                            +callChatGPT(promptChatGPT(request, "SN")) + "\n"
                            +callChatGPT(promptChatGPT(request, "TF")) + "\n"
                            +callChatGPT(promptChatGPT(request, "JP")) + "\n";

        System.out.println("========결과=========");
        System.out.println(mbtiContent);

        String[] mbtiArray = mbtiContent.split("\n");
        String[] mbtiE = mbtiArray[0].substring(4).split(", ");
        String[] mbtiI = mbtiArray[1].substring(4).split(", ");
        String[] mbtiS = mbtiArray[2].substring(4).split(", ");
        String[] mbtiN = mbtiArray[3].substring(4).split(", ");
        String[] mbtiT = mbtiArray[4].substring(4).split(", ");
        String[] mbtiF = mbtiArray[5].substring(4).split(", ");
        String[] mbtiJ = mbtiArray[6].substring(4).split(", ");
        String[] mbtiP= mbtiArray[7].substring(4).split(", ");


        double percentE = ((double) mbtiE.length / (mbtiE.length + mbtiI.length)) * 100;;
        double percentS = ((double) mbtiS.length / (mbtiS.length + mbtiN.length)) * 100;
        double percentT = ((double) mbtiT.length / (mbtiT.length + mbtiF.length)) * 100;
        double percentJ = ((double) mbtiJ.length / (mbtiJ.length + mbtiP.length)) * 100;

        Integer mbtiEInt = (int) Math.round(percentE);
        Integer mbtiSInt = (int) Math.round(percentS);
        Integer mbtiTInt = (int) Math.round(percentT);
        Integer mbtiJInt = (int) Math.round(percentJ);

        //BookMBTI 저장
        BookMBTI bookMBTI = BookMBTI.builder()
                .mbtiE(mbtiEInt)
                .mbtiS(mbtiSInt)
                .mbtiT(mbtiTInt)
                .mbtiJ(mbtiJInt)
                .build();

        //Book 저장
        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .publisher(request.getPublisher())
                .summary(request.getSummary())
                .content(request.getContent())
                .mbti(calculateMBTI(mbtiEInt, mbtiSInt, mbtiTInt, mbtiJInt))
                .bookMBTI(bookMBTI)
                .build();

        bookRepository.save(book);

        return BookMBTIResponse.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .publisher(request.getPublisher())
                .summary(request.getSummary())
                .mbti(calculateMBTI(mbtiEInt, mbtiSInt, mbtiTInt, mbtiJInt))
                .build();
    }

    //MBTI 계산 함수
    private String calculateMBTI(Integer mbtiE, Integer mbtiS, Integer mbtiT, Integer mbtiJ) {
        StringBuilder sb = new StringBuilder();
        sb.append(mbtiE>50 ? "E" : "I");
        sb.append(mbtiS>50 ? "S" : "N");
        sb.append(mbtiT>50 ? "T" : "F");
        sb.append(mbtiJ>50 ? "J" : "P");
        return sb.toString();
    }

    // Prompt
    private String promptChatGPT(BookMBTIRequest req, String mbti){
        String prompt = req.getSummary()
                        +", 이 문장 속의 단어인 키워드를 아래에서 (1)과 (2)에서 뽑아줘\n"
                        +"이 문장에 더 어울리는 쪽에서 키워드를 많이 추출해 주세요"
                        +"꼭 (1)과 (2)에서 하나 이상 있어야할 필요는 없으니까 그냥 맞는 키워드만 뽑아주면 돼\n\n";

        switch (mbti) {
            case "EI":
                prompt += "(1) : " + MBTIKeywords.E_KEYWORDS + "\n(2) : " + MBTIKeywords.I_KEYWORDS + "\n";
                break;
            case "SN":
                prompt += "(1) : " + MBTIKeywords.S_KEYWORDS + "\n(2) : " + MBTIKeywords.N_KEYWORDS + "\n";
                break;
            case "TF":
                prompt += "(1) : " + MBTIKeywords.T_KEYWORDS + "\n(2) : " + MBTIKeywords.F_KEYWORDS + "\n";
                break;
            case "JP":
                prompt += "(1) : " + MBTIKeywords.J_KEYWORDS + "\n(2) : " + MBTIKeywords.P_KEYWORDS + "\n";
                break;
            default:
                prompt = "";
                break;
        }
        prompt += "근데 뽑아줄 때 만약 예를들어 1성향의 키워드 '단어'를 뽑는다면 (1) 단어  이런 형태로 뽑아줘 꼭 '(1) 단어' 이 형태여야해!!! " +
                  "그리고 (1) 해당하는 거 주르륵, (2)에 해당하는 거 주르륵 이런 형태로 출력해줘!!!!\n" +
                  "(1) (2)에 뽑는 키워드 개수는 상관없이 그냥 어울리는 키워드 다 뽑아줘\n\n" +
                  "(1) 단어1, 단어2, 단어3, ...\n" +
                  "(2) 단어1, 단어2, 단어3, ... 꼭 이 형태로 출력해야돼\n" +
                  "단, (1)의 단어 개수와 (2)의 단어 개수가 다르게 해줘 둘 중 하나에 치중되면 좋겠어\n";
        return prompt;
    }

    // ChatGPT API 호출 및 응답 처리 함수
    private String callChatGPT(String prompt) {
        ChatGPTRequest chatGPTRequest = new ChatGPTRequest(model, prompt);
        ChatGPTResponse chatGPTResponse = template.postForObject(apiURL, chatGPTRequest, ChatGPTResponse.class);

        // 응답 검증
        if (chatGPTResponse == null || chatGPTResponse.getChoices().isEmpty()) {
            throw new RuntimeException("ChatGPT API 응답이 없습니다.");
        }

        // 응답에서 결과 추출
        return chatGPTResponse.getChoices().get(0).getMessage().getContent();
    }
}
