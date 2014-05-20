package org.jewzaam.bbsync;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.jpedal.examples.viewer.Commands;

import org.jpedal.examples.viewer.Viewer;
import org.sejda.impl.itext.MergeTask;
import org.sejda.model.exception.TaskException;
import org.sejda.model.input.PdfFileSource;
import org.sejda.model.input.PdfMergeInput;
import org.sejda.model.output.FileTaskOutput;
import org.sejda.model.parameter.MergeParameters;

/**
 * Based on http://files.idrsolutions.com/samplecode/org/jpedal/examples/easyintegration/PDFViewer.java.html#containers
 *
 * @author nmalik
 */
public class PDFViewer {

    private static final String DIR_DEVICE = "/media/SYNCSD/SYNC/FILES/SAVED/";
    private static final String DIR_STAGE = "/home/nmalik/Documents/bbsync/STAGE/";
    private static final String TMP_FILE = "/tmp/bbsync.pdf";

    private int tagFilenamesIndex = 0;
    private String[] tagFilenames;
    private String currentTagFilename;
    private JFrame frame;
    private Viewer viewer;
    private JTextField tags;

    public void initialize(Collection<String> tagFilenames) throws TaskException {
        MergeTask merge = new MergeTask();
        MergeParameters mp = new MergeParameters();
        for (String filename : tagFilenames) {
            File input = new File(filename);
            System.out.println("Adding to merge: " + filename);
            mp.addInput(new PdfMergeInput(PdfFileSource.newInstanceNoPassword(input)));
        }

        mp.setOverwrite(true);
        mp.setOutput(new FileTaskOutput(new File(TMP_FILE)));
        merge.before(mp);
        merge.execute(mp);

        this.tagFilenames = tagFilenames.toArray(new String[]{});

        //Create display frame
        frame = new JFrame();
        frame.getContentPane().setLayout(new BorderLayout());

        //Additional Label to show this is another program
        JLabel label = new JLabel("Type tags and hit enter: ");
        label.setFont(new Font("Lucida", Font.BOLD, 16));
        label.setForeground(Color.BLACK);

        SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMdd");
        String today = sdf.format(Calendar.getInstance().getTime());

        tags = new JTextField(today);
        tags.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    writeTags();
                    showNextPage();
                } catch (FileNotFoundException | UnsupportedEncodingException ex) {
                    Logger.getLogger(PDFViewer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        JButton back = new JButton("Back");
        back.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                showPreviousPage();
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(label, BorderLayout.WEST);
        panel.add(tags, BorderLayout.CENTER);
        panel.add(back, BorderLayout.EAST);

        frame.add(panel, BorderLayout.SOUTH);

        //Set up JFrame
        frame.setTitle("bbsync");
        frame.setSize(600, 900);
        frame.addWindowListener(new WindowListener() {
            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(1);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowOpened(WindowEvent e) {
            }
        });

        // initialize viewer
        resetViewer();

        // Display Frame
        frame.setVisible(true);

        // seed tags
        loadTags();
    }

    public void resetViewer() {
        // create main preview pane
        JInternalFrame rootContainer = new JInternalFrame("Preview Pane");

        // Setup the viewer
        viewer = new Viewer(rootContainer, null);
        // load properties from jar
        viewer.loadProperties("jar:/.properties.xml");
        viewer.setupViewer();
        viewer.getSwingGUI().addPageChangeListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                // new page inserted in view, load tags
                tagFilenamesIndex = viewer.getSwingGUI().getCurrentPage();
                loadTags();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        // Add the viewer to the frame
        frame.add(rootContainer, BorderLayout.CENTER);

        // Require for internalFrame to be displayed
        rootContainer.setVisible(true);

        // shouldn't do this here, but need it for now
        viewer.executeCommand(Commands.OPENFILE, new File[]{new File(TMP_FILE)});
    }

    public void showNextPage() {
        if (tagFilenamesIndex < -1) {
            // just to make sure it doesn't try to load a negative index
            tagFilenamesIndex = -1;
        }
        if (tagFilenamesIndex + 1 < tagFilenames.length) {
            // next page (page change event will load tags)
            ++tagFilenamesIndex;
            viewer.executeCommand(Commands.FORWARDPAGE, null);
        } else {
            // hopefully this clears the file loaded
            resetViewer();
        }
    }

    public void showPreviousPage() {
        if (0 <= tagFilenamesIndex - 1) {
            // previous page (page change event will load tags)
            --tagFilenamesIndex;
            viewer.executeCommand(Commands.BACKPAGE, null);
        }
    }

    public void loadTags() {
        // grab tag text and update tags
        currentTagFilename = this.tagFilenames[tagFilenamesIndex] + ".txt";

        try (FileReader fr = new FileReader(currentTagFilename);
                BufferedReader br = new BufferedReader(fr)) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            tags.setText(sb.toString());
        } catch (FileNotFoundException ex) {
            // just don't do anything, the tag file doesn't exist which is ok.
            // expected result: keep whatever text is in 'tags' UI element
        } catch (IOException ex) {
            Logger.getLogger(PDFViewer.class.getName()).log(Level.SEVERE, null, ex);
        }

        // request focus for tags
        tags.requestFocus();
    }

    public void writeTags() throws FileNotFoundException, UnsupportedEncodingException {
        String tagText = tags.getText();

        if (null != tagText && !tagText.isEmpty()) {
            System.out.printf("Writing '%s' to '%s'\n", tagText, currentTagFilename);
            try (PrintWriter writer = new PrintWriter(currentTagFilename, "UTF-8")) {
                writer.println(tagText);
            }
        }
    }

    /**
     * Copy any new files from device to stage directory.
     */
    public void copyToStage() throws IOException {
        File device = new File(DIR_DEVICE);
        if (device.exists()) {
            for (final File fileEntry : device.listFiles()) {
                if (!fileEntry.isDirectory()) {
                    // see if it exists already
                    String filename = DIR_STAGE + fileEntry.getName();
                    File f = new File(filename);
                    if (!f.exists()) {
                        // copy if not exist already
                        System.out.println("Copying: " + filename);
                        copyFile(fileEntry, f);
                    }
                }
            }
        }
    }

    /**
     * Get list of files that need tagged
     *
     * @return
     */
    public Collection<String> getFilesToTag() {
        Set<String> output = new TreeSet<>();

        File stage = new File(DIR_STAGE);

        if (!stage.exists()) {
            System.err.println("STAGE DOES NOT EXIST");
        }

        for (final File fileEntry : stage.listFiles()) {
            if (!fileEntry.isDirectory() && fileEntry.getName().endsWith(".PDF")) {
                // see if it exists already
                String filename = DIR_STAGE + fileEntry.getName();
                File f = new File(filename + ".txt");
                if (!f.exists()) {
                    System.out.println("Needs tagging: " + filename);
                    output.add(filename);
                }
            }
        }

        return output;
    }

    /**
     * http://stackoverflow.com/questions/106770/standard-concise-way-to-copy-a-file-in-java
     *
     * @param sourceFile
     * @param destFile
     * @throws IOException
     */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    public static void main(String[] args) throws IOException, TaskException {
        PDFViewer v = new PDFViewer();
        v.copyToStage();
        v.initialize(v.getFilesToTag());
    }
}
