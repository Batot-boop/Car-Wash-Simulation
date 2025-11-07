import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.LinkedList;

public class ServicesStation extends JFrame
{
    // Simulation Components
    private Semaphore full, empty, mutex, pumpBays;
    private Queue<String> queue;
    private boolean[] pumpStatus;
    private String[] carAtPump;
    private int carCounter = 1; 
    
    //GUI Components
    private JTextArea logArea;
    private JButton startButton;
    private JTextField waitingServicesField;
    private JTextField pumpsField;
    private JTextField numCarAddField;
    private JTable statusWaitingQueue;
    private JTable statusPumps;
    private DefaultTableModel waitingQueueModel;
    private DefaultTableModel pumpsModel;
    private ExecutorService executor;
    private boolean simulationRunning = false;

    // Constructor
    public ServicesStation()
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
        waitingServicesField = new JTextField(3);
        panel.add(waitingServicesField);
        
        panel.add(new JLabel("Pumps:"));
        pumpsField = new JTextField(3);
        panel.add(pumpsField);

        panel.add(new JLabel("Number of cars:"));
        numCarAddField = new JTextField(3);
        panel.add(numCarAddField);

        startButton = new JButton("Start Simulation");
        startButton.addActionListener(new StartButtonSimulation());
        panel.add(startButton);

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
        
        // Color renderer for Status column
        statusPumps.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (value != null) {
                    String status = value.toString().toUpperCase();
                    if (status.contains("FREE")) {
                        c.setForeground(Color.GREEN.darker());
                    } else if (status.contains("OCCUPIED")) {
                        c.setForeground(Color.RED);
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        });

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
        logArea.setText("Welcome to the Car Wash Simulation!");
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

    private void initializeSimulation()
    {
        int waitingCapacity = Integer.parseInt(waitingServicesField.getText());
        int numPumps = Integer.parseInt(pumpsField.getText());
        int numCars = Integer.parseInt(numCarAddField.getText());
        
        if (waitingCapacity < 1 || waitingCapacity > 10) 
        {
            JOptionPane.showMessageDialog(ServicesStation.this, 
            "Waiting area capacity must be between 1 and 10!", 
            "Invalid Input", 
            JOptionPane.ERROR_MESSAGE);
            return;
        }
        log("Simulation started with " + waitingCapacity + " waiting spots, " + numPumps + " pumps and " + numCars + " cars");
        
        // Initialize semaphores
        mutex = new Semaphore(1);
        empty = new Semaphore(waitingCapacity);
        full = new Semaphore(0);
        pumpBays = new Semaphore(numPumps);
        
        // Initialize data structures
        queue = new LinkedList<>();
        pumpStatus = new boolean[numPumps];
        carAtPump = new String[numPumps];
        
        // Initialize tables
        waitingQueueModel.setRowCount(0);
        pumpsModel.setRowCount(0);
        
        for (int i = 0; i < numPumps; i++) {
            pumpStatus[i] = false;
            carAtPump[i] = "Free";
            pumpsModel.addRow(new Object[]{"Pump " + (i+1),"FREE", "---"});
        }
        
        logArea.setText("");
        carCounter = 1;
        simulationRunning = true;
        
        // Create thread pool for pumps
        executor = Executors.newFixedThreadPool(numPumps);
        
        // Start pump threads
        for (int i = 0; i < numPumps; i++) {
            final int pumpId = i;
            executor.execute(new Pump(pumpId));
        }

        // Add initial cars
        for (int i = 0; i < numCars; i++) 
        {
            addNewCar();
        }
    }

    private void updateWaitingQueue()
    {
        SwingUtilities.invokeLater(() -> {
            waitingQueueModel.setRowCount(0);
            int position = 1;
            for (String car : queue) {
                waitingQueueModel.addRow(new Object[]{car, position++});
            }
        });
    }

    private void updatePumpTable(int pumpID, String status, String carID)
    {
        SwingUtilities.invokeLater(() -> {
            pumpsModel.setValueAt(status, pumpID, 1);
            pumpsModel.setValueAt(carID, pumpID, 2);
        });
    }

    class Semaphore 
    {
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

    // Car class (Producer)
    class Car extends Thread 
    {
        private final String carID;

        public Car(String id) {
            this.carID = id;
        }

        @Override
        public void run() {
            log("Car " + carID + " arrived.");

            empty.waitSem(); // Wait for empty space in queue
            mutex.waitSem(); // Enter critical section

            // add car to waiting queue
            queue.add(carID);
            log("Car " + carID + " entered waiting queue.");
            // updateQueueTable();

            mutex.signal();
            full.signal();
        }
    }

    // Pump class (Consumer)
    class Pump extends Thread 
    {
        private final int pumpId;
        private final Random rng = new Random();


        public Pump(int id) {
            this.pumpId = id;
        }
        
        @Override
        public void run() {
            while (simulationRunning) {
                try {
                    full.waitSem();   // Wait for cars in queue
                    pumpBays.waitSem();  // Wait for available pump
                    mutex.waitSem();  // Enter critical section
                    
                    // Remove car from queue
                    String carId = queue.poll();
                    updateWaitingQueue();
                    
                    mutex.signal();  // Exit critical section
                    empty.signal();  // Signal empty space available
                    
                    // Serve the car
                    pumpStatus[pumpId] = true;
                    carAtPump[pumpId] = carId;
                    updatePumpTable(pumpId,"OCCUPIED", carId);
                    
                    log("Pump " + (pumpId + 1) + ": " + carId + " Occupied");
                    log("Pump " + (pumpId + 1) + ": " + carId + " login");
                    log("Pump " + (pumpId + 1) + ": " + carId + " begins service at Bay " + (pumpId + 1));
                    
                    // Simulate service time
                    Thread.sleep(800 + rng.nextInt(1700));
                    
                    // Finish service
                    log("Pump " + (pumpId + 1) + ": " + carId + " finishes service");
                    log("Pump " + (pumpId + 1) + ": Bay " + (pumpId + 1) + " is now free");
                    
                    pumpStatus[pumpId] = false;
                    carAtPump[pumpId] = "Free";
                    updatePumpTable(pumpId,"FREE", "---");
                    
                    pumpBays.signal();  // Release the pump
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    class StartButtonSimulation implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                initializeSimulation();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(ServicesStation.this, 
                    "Invalid input! Please enter valid numbers.");
            }
        }
    }

    private void addNewCar()
    {
        if (simulationRunning)
        {
            String carID = "C" + carCounter++;
            new Thread(new Car(carID)).start();
        }
    }
    
    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(() -> {
            new ServicesStation().setVisible(true);
        });
    }
}