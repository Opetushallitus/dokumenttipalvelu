package fi.vm.sade.valinta.dokumenttipalvelu;

import fi.vm.sade.jetty.OpintopolkuJetty;

public class DokumenttiJetty extends OpintopolkuJetty {
    public static void main(String... args) {
        new DokumenttiJetty().start("/dokumenttipalvelu-service");
    }
}
