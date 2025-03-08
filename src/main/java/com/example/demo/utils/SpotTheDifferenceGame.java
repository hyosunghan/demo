package com.example.demo.utils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

public class SpotTheDifferenceGame extends JFrame {

    private static int GRID_WIDTH = 50;  // 魔方墙宽度
    private static int GRID_HEIGHT = 50; // 魔方墙高度
    private static final JTextField WIDTH_FIELD = new JTextField(String.valueOf(GRID_WIDTH), 5);
    private static final JLabel WIDTH_LABEL = new JLabel("宽度：");
    private static final JTextField HEIGHT_FIELD = new JTextField(String.valueOf(GRID_HEIGHT), 5);
    private static final JLabel HEIGHT_LABEL = new JLabel("高度：");
    private static final JButton RESTART_BUTTON = new JButton("重新开始");
    private static final Border BORDER = BorderFactory.createLineBorder(Color.BLACK, 1);
    private static final Dimension DIMENSION = new Dimension(1, 1);
    private static final Color[] COLORS = {
            Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.WHITE
    };
    private JButton[][] gridButtons1;
    private JButton[][] gridButtons2;
    private Point differencePoint;

    public SpotTheDifferenceGame() {
        RESTART_BUTTON.addActionListener(e -> {
            GRID_HEIGHT = Integer.parseInt(HEIGHT_FIELD.getText());
            GRID_WIDTH = Integer.parseInt(WIDTH_FIELD.getText());
            restartGame();
        });
        setTitle("魔方墙找茬");
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        setSize(screenSize.width, screenSize.height);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        paintAll();
    }

    private void restartGame() {
        // Clear the current panels
        getContentPane().removeAll();
        revalidate();
        repaint();

        paintAll();
    }

    private void paintAll() {
        JPanel inputPanel = new JPanel(new FlowLayout());

        inputPanel.add(WIDTH_LABEL);
        inputPanel.add(WIDTH_FIELD);
        inputPanel.add(HEIGHT_LABEL);
        inputPanel.add(HEIGHT_FIELD);
        inputPanel.add(RESTART_BUTTON);

        add(inputPanel, BorderLayout.NORTH);

        gridButtons1 = new JButton[GRID_HEIGHT][GRID_WIDTH];
        gridButtons2 = new JButton[GRID_HEIGHT][GRID_WIDTH];

        initializeGrid(gridButtons1, gridButtons2);
        generateDifferences();

        setVisible(true);
    }

    private void initializeGrid(JButton[][] gridButtons1, JButton[][] gridButtons2) {
        JPanel panel1 = new JPanel(new GridLayout(GRID_HEIGHT, GRID_WIDTH));
        JPanel panel2 = new JPanel(new GridLayout(GRID_HEIGHT, GRID_WIDTH));

        panel1.setBorder(BORDER);
        panel2.setBorder(BORDER);

        for (int i = 0; i < GRID_HEIGHT; i++) {
            for (int j = 0; j < GRID_WIDTH; j++) {
                JButton button1 = new JButton();
                JButton button2 = new JButton();
                Color color = getRandomColor();

                button1.setBackground(color);
                button1.setOpaque(true);
                button1.setBorderPainted(true);
                button1.setBorder(BORDER);
                button1.setPreferredSize(DIMENSION);

                button2.setBackground(color);
                button2.setOpaque(true);
                button2.setBorderPainted(true);
                button2.setBorder(BORDER);
                button2.setPreferredSize(DIMENSION);

                button1.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        handleMouseClick((JButton) e.getSource(), gridButtons1);
                    }
                });

                button2.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        handleMouseClick((JButton) e.getSource(), gridButtons2);
                    }
                });

                gridButtons1[i][j] = button1;
                gridButtons2[i][j] = button2;

                panel1.add(button1);
                panel2.add(button2);
            }
        }

        JPanel mainPanel = new JPanel(new GridLayout(1, 2));
        mainPanel.add(panel1);
        mainPanel.add(panel2);

        add(mainPanel, BorderLayout.CENTER);
    }

    private Color getRandomColor() {
        Random random = new Random();
        return COLORS[random.nextInt(COLORS.length)];
    }

    private void generateDifferences() {
        Random random = new Random();
        int x = random.nextInt(GRID_WIDTH);
        int y = random.nextInt(GRID_HEIGHT);

        differencePoint = new Point(x, y);

        // Ensure the color of the different block is different in both grids
        Color color1 = gridButtons1[y][x].getBackground();
        Color color2;
        do {
            color2 = getRandomColor();
        } while (color2.equals(color1));

        gridButtons2[y][x].setBackground(color2);
    }

    private void handleMouseClick(JButton button, JButton[][] gridButtons) {
        for (int i = 0; i < GRID_HEIGHT; i++) {
            for (int j = 0; j < GRID_WIDTH; j++) {
                if (button == gridButtons[i][j]) {
                    if (new Point(j, i).equals(differencePoint)) {
                        JOptionPane.showMessageDialog(this, "找茬成功！");
                        restartGame();
                    }
                    return;
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SpotTheDifferenceGame::new);
    }
}
