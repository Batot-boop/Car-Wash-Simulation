import javax.swing.*;
import java.awt.*;
import java.util.*;

class Semaphore {
    private int value;

    public Semaphore(int value) {
        this.value = value;
    }

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
    // Constructor
    public Synchronization()
    {
        setTitle("Car Wash Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        createComponents();
        LayoutComponents();
        setupEventHandlers();
    }

    // Initialize UI components
    private void createComponents() {}

    // Layout the components in the frame
    private void LayoutComponents() {}

    // Setup event handlers for components
    private void setupEventHandlers() {}

    public static void main(String[] args) {}
}
