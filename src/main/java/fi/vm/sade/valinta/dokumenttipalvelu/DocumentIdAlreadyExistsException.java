package fi.vm.sade.valinta.dokumenttipalvelu;

public class DocumentIdAlreadyExistsException extends RuntimeException {
  public DocumentIdAlreadyExistsException(String message) {
    super(message);
  }
}
