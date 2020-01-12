import io.nayasis.basica.base.Strings;
import io.nayasis.basica.cli.CommandExecutor;
import io.nayasis.basica.file.Files;
import io.nayasis.basica.model.NMap;
import io.nayasis.basicafx.javafx.etc.Threads;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
public class Batch {

    @Test
    public void makeChd() {

        makeChd( "e:/download/ggg", "**/*.cue" );

    }


    int i = 0;

    private void makeChd( String root, String... pattern ) {

        List<Path> file = Files.findFile(root, -1, pattern);

        file.forEach(cuePath -> {

            Map param = new NMap<>();
            param.put( "filePath", cuePath.toString() );
            param.put( "dir", cuePath.getParent().toString() );
            param.put( "name", Files.removeExtension(cuePath.getFileName()) );

            String cmd = Strings.format("\\\\NAS\\emul\\_tool\\chd\\chdman.exe createcd -i \"{filePath}\" -o \"{dir}\\", param) + Strings.format("{name}.chd\"", param);

            i++;

            System.out.println( Strings.format(">> {}/{} : ratio : {}", i, file.size(), i / file.size() * 100  ) );
            System.out.println( cmd );

            new CommandExecutor().run( cmd ).waitFor();

        });


    }

}
