package nl.mantiscode.csvreader;

import au.com.bytecode.opencsv.CSVReader;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.StreamVariable;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Html5File;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import javax.servlet.annotation.WebServlet;

@Push
@Theme("mytheme")
@SuppressWarnings("serial")
public class MyVaadinUI extends UI {

    private ProgressBar progress;
    private Table table;

    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = MyVaadinUI.class, widgetset = "nl.mantiscode.csvreader.AppWidgetSet")
    public static class Servlet extends VaadinServlet {
    }

    @Override
    protected void init(VaadinRequest request) {

        final VerticalLayout rootLayout = new VerticalLayout();
        setContent(rootLayout);
                
        final Label infoLabel = new Label("drop csv file(s) here");
        final VerticalLayout dropPane = new VerticalLayout(infoLabel);
        dropPane.setMargin(false);
        dropPane.setComponentAlignment(infoLabel, Alignment.MIDDLE_RIGHT);
        dropPane.setWidth(300.0f, Unit.PIXELS);
        dropPane.setHeight(200.0f, Unit.PIXELS);
        dropPane.addStyleName("drop-area");

        final ImageDropBox dropBox = new ImageDropBox(dropPane);
        dropBox.setDropHandler(dropBox);
        dropBox.setSizeUndefined();

        progress = new ProgressBar();
        progress.setVisible(false);
        progress.setSizeFull();

        rootLayout.addComponent(progress);
        rootLayout.addComponent(dropBox);

    }

    private class ImageDropBox extends DragAndDropWrapper implements
            DropHandler {
        
        public ImageDropBox(final Component root) {
            super(root);
        }

        @Override
        public void drop(final DragAndDropEvent dropEvent) {

            // expecting this to be an html5 drag
            final DragAndDropWrapper.WrapperTransferable tr = (DragAndDropWrapper.WrapperTransferable) dropEvent
                    .getTransferable();
            final Html5File[] files = tr.getFiles();
            if (files != null) {
                for (final Html5File html5File : files) {

                    final String fileName = html5File.getFileName();
                    final long fileSize = html5File.getFileSize();

                    if (html5File.getType().equalsIgnoreCase("text/csv")) {

                        final ByteArrayOutputStream bas = new ByteArrayOutputStream();
                        final StreamVariable streamVariable = new StreamVariable() {

                            @Override
                            public OutputStream getOutputStream() {
                                return bas;
                            }

                            @Override
                            public boolean listenProgress() {
                                return true;
                            }

                            @Override
                            public void onProgress(final StreamVariable.StreamingProgressEvent event) {
                                float current = (100.0f / fileSize) * event.getBytesReceived();
                                progress.setValue(current / 100);
                            }

                            @Override
                            public void streamingStarted(final StreamVariable.StreamingStartEvent event) {
                            }

                            @Override
                            public void streamingFinished(final StreamVariable.StreamingEndEvent event) {
                                progress.setVisible(false);
                                showCsv(fileName, html5File.getFileName(), bas);
                            }

                            @Override
                            public void streamingFailed(final StreamVariable.StreamingErrorEvent event) {
                                progress.setVisible(false);
                            }

                            @Override
                            public boolean isInterrupted() {
                                return false;
                            }
                        };

                        html5File.setStreamVariable(streamVariable);
                        progress.setVisible(true);
                    } else {
                        Notification.show("Only CSV files are allowed, uploaded type is: " + html5File.getType());
                    }
                }

            } else {
                final String text = tr.getText();
                if (text != null) {
                    showText(text);
                }
            }
        }

        private void showText(final String text) {
            showComponent(new Label(text), "Wrapped text content");
        }

        private void showCsv(final String name, final String type,
                final ByteArrayOutputStream bas) {
            
            table = new Table();
            table.setCaption(name);
            try {
                /* Let's build a container from the CSV File */
                final byte[] byteArray = bas.toByteArray();
                IndexedContainer indexedContainer;
                try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(byteArray))) {
                    indexedContainer = buildContainerFromCSV(reader);
                }

                table.setContainerDataSource(indexedContainer);
                
            } catch (IOException e) {
            }
            
            table.setSizeFull();
            table.setSizeUndefined();
            
            showComponent(table, name);
        }

        private void showComponent(final Component c, final String name) {
            final VerticalLayout layout = new VerticalLayout();
            layout.setSizeUndefined();
            layout.setMargin(true);
            final Window w = new Window(name, layout);
            w.addStyleName("dropdisplaywindow");
            w.setSizeUndefined();
            w.setResizable(true);
            
            layout.addComponent(c);
            UI.getCurrent().addWindow(w);

        }

        @Override
        public AcceptCriterion getAcceptCriterion() {
            return AcceptAll.get();
        }
    }

    /**
     * Uses http://opencsv.sourceforge.net/ to read the entire contents of a CSV
     * file, and creates an IndexedContainer from it
     *
     * @param reader
     * @return
     * @throws IOException
     */
    protected IndexedContainer buildContainerFromCSV(Reader reader) throws IOException {
        IndexedContainer container = new IndexedContainer();
        CSVReader csvReader = new CSVReader(reader);
        String[] columnHeaders = null;
        String[] record;
        while ((record = csvReader.readNext()) != null) {
            if (columnHeaders == null) {
                columnHeaders = record;
                addItemProperties(container, columnHeaders);
            } else {
                addItem(container, columnHeaders, record);
            }
        }
        return container;
    }

    /**
     * Set's up the item property ids for the container. Each is a String (of
     * course, you can create whatever data type you like, but I guess you need
     * to parse the whole file to work it out)
     *
     * @param container The container to set
     * @param columnHeaders The column headers, i.e. the first row from the CSV
     * file
     */
    private static void addItemProperties(IndexedContainer container, String[] columnHeaders) {
        for (String propertyName : columnHeaders) {
            container.addContainerProperty(propertyName, String.class, null);
        }
    }

    /**
     * Adds an item to the given container, assuming each field maps to it's
     * corresponding property id. Again, note that I am assuming that the field
     * is a string.
     *
     * @param container
     * @param propertyIds
     * @param fields
     */
    private static void addItem(IndexedContainer container, String[] propertyIds, String[] fields) {
        if (propertyIds.length != fields.length) {
            throw new IllegalArgumentException("Different number of columns to fields in the record");
        }
        Object itemId = container.addItem();
        Item item = container.getItem(itemId);
        for (int i = 0; i < fields.length; i++) {
            String propertyId = propertyIds[i];
            String field = fields[i];
            item.getItemProperty(propertyId).setValue(field);
        }
    }
}
