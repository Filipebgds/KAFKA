
package org.example.cliente;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.Callback;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.awt.*;

public class NotificationUI extends JFrame {
    private JTextArea notificationDisplayArea;
    private JTextField messageInputField;
    private JRadioButton priorityHighRadio;
    private JRadioButton priorityLowRadio;
    private JButton sendButton;
    private KafkaProducer<String, String> kafkaProducer;

    public NotificationUI() {
        setTitle("Sistema de Notificações Acadêmicas");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridBagLayout()); // Usando GridBagLayout ao invés de BorderLayout
        setLocationRelativeTo(null);

        configureKafkaProducer();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;

        JPanel inputPanel = createInputPanel(); // Painel de entrada

        notificationDisplayArea = new JTextArea();
        notificationDisplayArea.setEditable(false);
        notificationDisplayArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); // Alterando a fonte
        notificationDisplayArea.setLineWrap(true);
        notificationDisplayArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(notificationDisplayArea);

        TitledBorder border = BorderFactory.createTitledBorder("Notificações");
        border.setTitleFont(new Font("Serif", Font.BOLD, 14)); // Alterando a fonte do título
        scrollPane.setBorder(border);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0.5;
        add(inputPanel, gbc); // Mudando a posição do painel de entrada

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(scrollPane, gbc); // Colocando a área de notificações ao lado do painel de entrada

        setVisible(true);
    }

    private void configureKafkaProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProducer = new KafkaProducer<>(props);
    }

    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(4, 1, 10, 10)); // Alterando o layout do painel de entrada
        inputPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Enviar Notificações",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                new Font("Serif", Font.BOLD, 14)
        ));

        JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Painel para a mensagem
        messagePanel.add(new JLabel("Mensagem: "));
        messageInputField = new JTextField(20); // Alterando o tamanho do campo de texto
        messagePanel.add(messageInputField);

        JPanel priorityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Painel para a prioridade
        priorityPanel.add(new JLabel("Prioridade: "));
        priorityHighRadio = new JRadioButton("Alta");
        priorityLowRadio = new JRadioButton("Baixa");
        ButtonGroup priorityGroup = new ButtonGroup();
        priorityGroup.add(priorityHighRadio);
        priorityGroup.add(priorityLowRadio);
        priorityPanel.add(priorityHighRadio);
        priorityPanel.add(priorityLowRadio);


        JPanel sendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Painel para o botão de enviar
        sendButton = new JButton("Enviar");
        sendPanel.add(sendButton);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        inputPanel.add(messagePanel); // Adicionando o painel da mensagem
        inputPanel.add(priorityPanel); // Adicionando o painel da prioridade
        inputPanel.add(sendPanel); // Adicionando o painel do botão de enviar

        return inputPanel;
    }

    private List<String> lowPriorityBatch = new ArrayList<>();

    public void sendMessage() {
        String message = messageInputField.getText().trim();
        String topic = "notifications";
        Integer priority = 0;
        if (priorityHighRadio.isSelected()) {
            priority = 1;
        } else {
            priority = 2;
        }
        String messageWithPriority = message + " (Prioridade: " + priority + ")";

        if (priority == 1) {
            kafkaProducer.send(new ProducerRecord<>(topic, messageWithPriority), new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception != null) {
                        JOptionPane.showMessageDialog(NotificationUI.this,
                                "Erro ao enviar a notificação: " + exception.getMessage(),
                                "Erro",
                                JOptionPane.ERROR_MESSAGE);
                    } else {
                        displayNotification("Enviado instantaneamente: " + messageWithPriority);
                    }
                }
            });
        } else if (priority == 2) {
            lowPriorityBatch.add(messageWithPriority);

            if (lowPriorityBatch.size() == 5) {
                String batchedMessages = String.join(", ", lowPriorityBatch);
                kafkaProducer.send(new ProducerRecord<>(topic, batchedMessages), new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception exception) {
                        if (exception != null) {
                            JOptionPane.showMessageDialog(NotificationUI.this,
                                    "Erro ao enviar o lote de notificações: " + exception.getMessage(),
                                    "Erro",
                                    JOptionPane.ERROR_MESSAGE);
                        } else {
                            displayNotification("Lote de 5 mensagens enviado: " + batchedMessages);
                        }
                    }
                });
                lowPriorityBatch.clear();
            } else {
                displayNotification("Mensagem de baixa prioridade armazenada!");
            }
        }
    }

    private void displayNotification(String notification) {
        notificationDisplayArea.append(notification + "\n");
    }

    @Override
    public void dispose() {
        kafkaProducer.close();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(NotificationUI::new);
    }
}