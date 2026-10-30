package I2new.group2.ui;

import I2new.group2.dao.DatabaseInitializer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Top level menu: options a-e are sub-menus, run through the abstract Menu type. */
public class MainMenu extends Menu {

    private final List<Menu> subMenus;

    public MainMenu(ConsoleInput in, List<Menu> subMenus) {
        super(in);
        this.subMenus = List.copyOf(subMenus);
    }

    @Override
    public String getTitle() {
        return "CONSERVATOIRE VIRTUEL";
    }

    @Override
    protected List<String> getOptions() {
        List<String> options = new ArrayList<>();
        char letter = 'a';
        for (Menu menu : subMenus) {
            options.add(letter++ + ") " + menu.getTitle());
        }
        options.add("Reset demo data");
        return options;
    }

    @Override
    protected String getExitLabel() {
        return "Quit";
    }

    @Override
    protected void handle(int choice) throws SQLException {
        if (choice <= subMenus.size()) {
            subMenus.get(choice - 1).run(); // dynamic dispatch on the concrete menu
            return;
        }
        if (in.readYesNo("This deletes every change and reloads the demo scenario. Continue?")) {
            DatabaseInitializer.resetDemoData();
            System.out.println("Demo data reloaded.");
        }
    }
}
