import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class Semaphore {
    private int value;

    public Semaphore(int value) {
        this.value = value;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public synchronized void waitSem() {
        while (value <= 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        value--;
    }

    public synchronized void signal() {
        value++;
        notify();
    }
}

public class Synchronization extends JFrame
{
    //GUI Components
    private JTextArea logArea;
    private JButton startButton;
    private JButton addCarButton;
    private JTextField waitingServicesField;
    private JTextField pumpsField;
    private JTable statusWaitingQueue;
    private JTable statusPumps;
    private DefaultTableModel waitingQueueModel;
    private DefaultTableModel pumpsModel;

    // Constructor
    public Synchronization()
    {
        initializeGUI();
    }

    private void initializeGUI()
    {
        setTitle("Car Wash Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800,600);
        setLocationRelativeTo(null);

        // Create main panel with border layout
        setLayout(new BorderLayout(10,10));

        // Input Panel
        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.NORTH);

        // Center Panel
        JPanel centerPanel = createCenterPanel();
        add(centerPanel, BorderLayout.CENTER);

        // Log Panel
        JPanel logPanel = createLogPanel();
        add(logPanel, BorderLayout.SOUTH);

        // Add padding
        ((JComponent) getContentPane()).setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );
    }

    private JPanel createInputPanel() 
    {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        panel.add(new JLabel("Waiting Services:"));
        waitingServicesField = new JTextField(5);
        panel.add(waitingServicesField);
        
        panel.add(new JLabel("Pumps:"));
        pumpsField = new JTextField(5);
        panel.add(pumpsField);

        startButton = new JButton("Start Simulation");
        startButton.addActionListener(new StartButtonSimulation());
        panel.add(startButton);

        addCarButton = new JButton("Add Car");
        addCarButton.setEnabled(false);
        panel.add(addCarButton);

        panel.setBorder(BorderFactory.createTitledBorder("Controllers"));

        return panel;
    }
    
    private JPanel createCenterPanel() 
    {
        JPanel panel = new JPanel(new GridLayout(1,2,10,10));
        
        // Waiting Queue Table
        waitingQueueModel = new DefaultTableModel(new String[]{"Car ID", "Position"}, 0);
        statusWaitingQueue = new JTable(waitingQueueModel);
        JScrollPane waitingQueueScrollPane = new JScrollPane(statusWaitingQueue);
        waitingQueueScrollPane.setBorder(BorderFactory.createTitledBorder("Waiting Queue"));

        // Pumps Table
        pumpsModel = new DefaultTableModel(new String[]{"Pump","Status","Current Car"}, 0);
        statusPumps = new JTable(pumpsModel);
        JScrollPane pumpsScrollPane = new JScrollPane(statusPumps);
        pumpsScrollPane.setBorder(BorderFactory.createTitledBorder("Pumps Status"));

        panel.add(waitingQueueScrollPane);
        panel.add(pumpsScrollPane);

        return panel;
    }
    
    private JPanel createLogPanel() 

    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Log Avtivity"));

        logArea = new JTextArea(15,70);
        logArea.setEditable(false);
        logArea.setFont(new Font("Cascadia Code", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void log(String message)
    {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void updatePumpTable(int pumpID, String status, String carID)
    {
        SwingUtilities.invokeLater(() -> {
            pumpsModel.setValueAt(status, pumpID, 1);
            pumpsModel.setValueAt(carID, pumpID, 2);
        });
    }

    class StartButtonSimulation implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            try {
                startButton.setEnabled(false);
                addCarButton.setEnabled(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(Synchronization.this,
                "Invalid input! Please enter valid numbers.");
            }
        }
    }

    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(() -> {
            new Synchronization().setVisible(true);
        });
    }
}