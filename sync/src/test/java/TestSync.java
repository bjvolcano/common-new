import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

@Slf4j
public class TestSync {

    @Value("${spring.application.name}")
    protected String applicationName;

    public static void main(String[] args) {
        TestSync sync = new TestSync();

        log.info("a : {}", "");
    }

}
