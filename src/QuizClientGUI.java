import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Properties;

public class QuizClientGUI {
    private static String SERVER_IP;
    private static int PORT;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private JFrame frame = new JFrame("Quiz Game Client");
    private JTextArea questionArea = new JTextArea(10, 30);
    private JTextField answerField = new JTextField(15);
    private JButton submitButton = new JButton("Submit");
    private JButton nextButton = new JButton("Next");
    private JLabel scoreLabel = new JLabel("Score: 0");

    private boolean waitingForNext = false;
    private String nextQuestion = "";
    private boolean isFirstQuestion = true;
    private int score = 0;  // 현재 클라이언트의 점수를 저장하는 변수

    public QuizClientGUI() {
        loadServerConfig();  // server_info.dat 파일에서 서버 IP와 포트를 로드

        questionArea.setEditable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 350);
        frame.setLayout(new BorderLayout());

        JPanel questionPanel = new JPanel(new BorderLayout());
        questionPanel.add(new JScrollPane(questionArea), BorderLayout.CENTER);
        
        JPanel answerPanel = new JPanel();
        answerPanel.add(new JLabel("Answer: "));
        answerPanel.add(answerField);
        answerPanel.add(submitButton);
        
        questionPanel.add(answerPanel, BorderLayout.SOUTH);

        JPanel controlPanel = new JPanel();
        controlPanel.add(nextButton);
        controlPanel.add(scoreLabel);

        frame.add(questionPanel, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);

        submitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendAnswer();  // 답변 제출
            }
        });

        nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (waitingForNext) {  // 다음 질문으로 넘어갈 준비가 되었는지 확인
                    questionArea.setText(nextQuestion);
                    answerField.setText("");  // 답변 필드 초기화
                    waitingForNext = false;
                    submitButton.setEnabled(true);  // 제출 버튼 활성화
                    nextButton.setEnabled(false);  // 다음 버튼 비활성화
                }
            }
        });

        nextButton.setEnabled(false);
        
        frame.setVisible(true);
        connectToServer();  // 서버에 연결
    }

    private void loadServerConfig() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("server_info.dat")) {
            props.load(fis);
            SERVER_IP = props.getProperty("IP", "localhost");  // IP 설정 불러오기
            PORT = Integer.parseInt(props.getProperty("PORT", "1234"));  // 포트 설정 불러오기
            System.out.println("Loaded server config: IP=" + SERVER_IP + ", PORT=" + PORT);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to load server configuration. Using defaults.");
            SERVER_IP = "localhost";
            PORT = 1234;
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, PORT);  // 설정된 IP와 포트로 서버 연결
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));  // 서버로부터 입력 스트림 생성
            out = new PrintWriter(socket.getOutputStream(), true);  // 서버로 출력 스트림 생성
            System.out.println("Connected to the server.");
            new Thread(new ServerListener()).start();  // 서버 응답을 처리하는 스레드 시작
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendAnswer() {
        String answer = answerField.getText();  // 사용자 입력값 읽기
        out.println(answer);  // 서버로 답변 전송
        System.out.println("Sending answer to server: " + answer);  // 콘솔에 제출한 답변 출력
        submitButton.setEnabled(false);  // 제출 후 제출 버튼 비활성화
    }

    private class ServerListener implements Runnable {
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("Your final score is:")) {
                        scoreLabel.setText(message);  // 최종 점수 표시
                        System.out.println(message);  // 최종 점수 콘솔 출력
                        submitButton.setEnabled(false);
                        answerField.setEnabled(false);
                        nextButton.setEnabled(false);
                    } else if (message.equals("Correct!")) {
                        questionArea.append("\n" + message + "\n");
                        System.out.println(message);  // 정답 여부 콘솔 출력
                        score++;  // 정답 시 점수 증가
                        scoreLabel.setText("Score: " + score);  // 실시간 점수 표시
                        nextButton.setEnabled(true);
                        waitingForNext = true;
                    } else if (message.startsWith("Incorrect")) {
                        questionArea.append("\n" + message + "\n");
                        System.out.println(message);  // 오답 여부 콘솔 출력
                        nextButton.setEnabled(true);
                        waitingForNext = true;
                    } else {
                        if (isFirstQuestion) {
                            questionArea.setText(message);  // 첫 번째 질문 표시
                            System.out.println("Received question from server: " + message);  // 첫 질문 콘솔 출력
                            isFirstQuestion = false;
                        } else {
                            nextQuestion = message;  // 다음 질문 저장
                            System.out.println("Next question received: " + message);  // 다음 질문 콘솔 출력
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuizClientGUI());
    }
}
