import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class QuizServer {
    private static final int PORT = 1234;  // 서버가 사용할 포트 번호
    private static final List<String> questions = Arrays.asList(
        "Who won the Nobel Prize in Literature in 2024?",
        "Who won the U.S. Presidential Election in 2024?",
        "Which city is hosting the 2024 Summer Olympics?"
    );  // 퀴즈 질문 목록
    private static final List<String> answers = Arrays.asList(
        "Han Kang",
        "Donald Trump",
        "Paris"
    );  // 각 질문에 대한 정답 목록

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(10);  // 최대 10개의 클라이언트를 처리할 수 있는 스레드 풀 생성
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Quiz server started...");
            while (true) {
                // 클라이언트가 연결될 때까지 대기하고, 연결되면 클라이언트를 처리할 스레드를 할당
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected.");
                pool.execute(new ClientHandler(clientSocket));  // 클라이언트 처리 스레드 실행
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));  // 클라이언트로부터 입력을 읽는 스트림
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);  // 클라이언트로 데이터를 보내는 출력 스트림
            ) {
                int score = 0;  // 클라이언트의 점수를 저장하는 변수
                for (int i = 0; i < questions.size(); i++) {
                    // 각 질문을 클라이언트에게 전송
                    out.println("Question " + (i + 1) + ": " + questions.get(i));
                    System.out.println("Sending question " + (i + 1) + " to client: " + questions.get(i));  // 질문 전송 로그 추가
                    String response = in.readLine();  // 클라이언트의 응답을 읽음

                    if (response == null) {
                        System.out.println("Client disconnected.");
                        break;  // 클라이언트가 연결을 끊으면 종료
                    }

                    System.out.println("Received answer from client: " + response);  // 응답 수신 로그 추가

                    // 정답 확인 후 피드백을 클라이언트에게 전송
                    if (response.equalsIgnoreCase(answers.get(i))) {
                        out.println("Correct!");
                        score++;
                    } else {
                        out.println("Incorrect. The correct answer is: " + answers.get(i));
                    }
                    out.println("Please press 'Next' for the next question.");  // 다음 질문 요청 메시지 전송
                }
                out.println("Your final score is: " + score);  // 최종 점수를 클라이언트에게 전송
                System.out.println("Final score sent to client: " + score);  // 최종 점수 전송 로그 추가
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();  // 클라이언트 소켓 닫기
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
