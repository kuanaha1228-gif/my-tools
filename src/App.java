import java.util.Scanner;
import java.util.LinkedHashSet;
import java.util.Set;

public class App {

    private static final String SCHEMA = "ods";
    private static final String OWNER = "admin";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== OA Cleanup SQL Generator ===");
        System.out.println("Enter table names (comma or space separated), or 'exit' to quit:");
        System.out.println("Examples: ods_oa_formmain_6954  or  ods_oa_formmain_6954,ods_oa_formdetail_6954");
        System.out.println();

        while (true) {
            String input;
            try {
                System.out.print("> ");
                input = scanner.nextLine().trim();
            } catch (java.util.NoSuchElementException e) {
                break;
            }

            if (input.isEmpty()) {
                continue;
            }
            if (input.equalsIgnoreCase("exit")) {
                break;
            }

            String[] parts = input.split("[,\\s]+");
            Set<String> seen = new LinkedHashSet<>();
            for (String part : parts) {
                String name = part.trim();
                if (name.isEmpty()) continue;
                if (name.startsWith(SCHEMA + ".")) {
                    name = name.substring(SCHEMA.length() + 1);
                }
                seen.add(name);
            }

            StringBuilder output = new StringBuilder();
            boolean first = true;
            for (String tableName : seen) {
                if (!first) {
                    output.append("\n");
                }
                first = false;
                output.append(generateCleanupTable(tableName));
                output.append("\n\n");
                output.append(generateCleanupProcedure(tableName));
                output.append("\n");
            }

            System.out.println();
            System.out.println("--- Generated SQL ---");
            System.out.println(output);
            System.out.println("--- End ---");
            System.out.println();
        }

        scanner.close();
        System.out.println("Bye.");
    }

    private static String generateCleanupTable(String tableName) {
        String fullTableName = SCHEMA + "." + tableName;
        String cleanupTableName = fullTableName + "_id_cleanup";
        String pkName = "pk_" + tableName + "_id_cleanup";

        return String.format("""
                -- Table: %s
                create table if not exists %s
                (
                    id                bigint not null,
                    constraint %s primary key (id)
                );""",
                tableName, cleanupTableName, pkName);
    }

    private static String generateCleanupProcedure(String tableName) {
        String fullTableName = SCHEMA + "." + tableName;
        String cleanupTableName = fullTableName + "_id_cleanup";
        String procedureName = "clean_" + tableName;

        return String.format("""
                create procedure %s.%s()
                    language plpgsql
                as
                $$
                BEGIN
                    DELETE FROM %s t
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM %s c
                        WHERE c.id = t.id
                    );
                END;
                $$;

                alter procedure %s.%s() owner to %s;""",
                SCHEMA, procedureName,
                fullTableName,
                cleanupTableName,
                SCHEMA, procedureName, OWNER);
    }
}
