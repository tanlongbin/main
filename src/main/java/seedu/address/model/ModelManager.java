package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.model.person.Person;
import seedu.address.model.person.exceptions.PersonNotFoundException;

/**
 * Represents the in-memory model of the address book data.
 */
public class ModelManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final VersionedAddressBook versionedAddressBook;
    private final VersionedArchiveBook versionedArchiveBook;
    private final VersionedPinBook versionedPinBook;
    private final UserPrefs userPrefs;
    private final FilteredList<Person> filteredPersons;
    private final FilteredList<Person> filteredArchivedPersons;
    private final FilteredList<Person> filteredPinnedPersons;
    private final SimpleObjectProperty<Person> selectedPerson = new SimpleObjectProperty<>();

    /**
     * Initializes a ModelManager with the given addressBook and userPrefs.
     */
    public ModelManager(ReadOnlyAddressBook addressBook, ReadOnlyArchiveBook archiveBook,
                        ReadOnlyPinBook pinBook, ReadOnlyUserPrefs userPrefs) {
        super();
        requireAllNonNull(addressBook, archiveBook, pinBook, userPrefs);

        logger.fine("Initializing with address book: " + addressBook
                + "Initializing with archive book: " + archiveBook
                + "Initializing with pin book" + pinBook
                + "and user prefs " + userPrefs);

        versionedAddressBook = new VersionedAddressBook(addressBook);
        versionedArchiveBook = new VersionedArchiveBook(archiveBook);
        versionedPinBook = new VersionedPinBook(pinBook);
        this.userPrefs = new UserPrefs(userPrefs);
        filteredPersons = new FilteredList<>(versionedAddressBook.getPersonList());
        filteredPersons.addListener(this::ensureSelectedPersonIsValid);
        filteredArchivedPersons = new FilteredList<>(versionedArchiveBook.getPersonList());
        filteredArchivedPersons.addListener(this::ensureSelectedPersonIsValid);
        filteredPinnedPersons = new FilteredList<>(versionedPinBook.getPersonList());
        filteredPinnedPersons.addListener(this::ensureSelectedPersonIsValid);
    }

    public ModelManager() {
        this(new AddressBook(), new ArchiveBook(), new PinBook(), new UserPrefs());
    }

    //=========== UserPrefs ==================================================================================

    @Override
    public void setUserPrefs(ReadOnlyUserPrefs userPrefs) {
        requireNonNull(userPrefs);
        this.userPrefs.resetData(userPrefs);
    }

    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        userPrefs.setGuiSettings(guiSettings);
    }

    @Override
    public Path getAddressBookFilePath() {
        return userPrefs.getAddressBookFilePath();
    }

    @Override
    public void setAddressBookFilePath(Path addressBookFilePath) {
        requireNonNull(addressBookFilePath);
        userPrefs.setAddressBookFilePath(addressBookFilePath);
    }

    @Override
    public Path getArchiveBookFilePath() {
        return userPrefs.getArchiveBookFilePath();
    }

    @Override
    public void setArchiveBookFilePath(Path archiveBookFilePath) {
        requireNonNull(archiveBookFilePath);
        userPrefs.setArchiveBookFilePath(archiveBookFilePath);
    }

    @Override
    public Path getPinBookFilePath() {
        return userPrefs.getPinBookFilePath();
    }

    @Override
    public void setPinBookFilePath(Path pinBookFilePath) {
        requireNonNull(pinBookFilePath);
        userPrefs.setPinBookFilePath(pinBookFilePath);
    }

    //=========== AddressBook ================================================================================

    @Override
    public void setAddressBook(ReadOnlyAddressBook addressBook) {
        versionedAddressBook.resetData(addressBook);
    }

    @Override
    public ReadOnlyAddressBook getAddressBook() {
        return versionedAddressBook;
    }

    @Override
    public boolean hasPerson(Person person) {
        requireNonNull(person);
        return versionedAddressBook.hasPerson(person);
    }

    @Override
    public void deletePerson(Person target) {
        versionedAddressBook.removePerson(target);
    }

    @Override
    public void addPerson(Person person) {
        versionedAddressBook.addPerson(person);
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
    }

    @Override
    public void setPerson(Person target, Person editedPerson) {
        requireAllNonNull(target, editedPerson);

        versionedAddressBook.setPerson(target, editedPerson);
    }

    //=========== ArchiveBook ================================================================================

    @Override
    public void setArchiveBook(ReadOnlyArchiveBook archiveBook) {
        versionedArchiveBook.resetData(archiveBook);
    }

    @Override
    public ReadOnlyArchiveBook getArchiveBook() {
        return versionedArchiveBook;
    }

    @Override
    public void archivePerson(Person target) {
        versionedArchiveBook.addPerson(target);
        versionedAddressBook.removePerson(target);
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
    }

    @Override
    public void unarchivePerson(Person target) {
        versionedAddressBook.addPerson(target);
        versionedArchiveBook.removePerson(target);
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
    }

    //=========== PinBook ====================================================================================

    @Override
    public void setPinBook(ReadOnlyPinBook pinBook) {
        versionedPinBook.resetData(pinBook);
    }

    @Override
    public ReadOnlyPinBook getPinBook() {
        return versionedPinBook;
    }

    @Override
    public void pinPerson(Person target) {
        versionedPinBook.addPerson(target);
        versionedAddressBook.removePerson(target);
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
    }

    @Override
    public void unpinPerson(Person target) {
        versionedPinBook.removePerson(target);
        versionedAddressBook.addPerson(target);
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
    }

    //=========== Filtered Person List Accessors =============================================================

    /**
     * Returns an unmodifiable view of the list of {@code Person} backed by the internal list of
     * {@code versionedAddressBook}
     */
    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return filteredPersons;
    }

    @Override
    public void updateFilteredPersonList(Predicate<Person> predicate) {
        requireNonNull(predicate);
        filteredPersons.setPredicate(predicate);
    }

    //=========== Filtered Archived Person List Accessors ===================================================

    /**
     * Returns an unmodifiable view of the list of {@code Person} backed by the internal list of
     * {@code versionedArchiveBook}
     */
    @Override
    public ObservableList<Person> getFilteredArchivedPersonList() {
        return filteredArchivedPersons;
    }

    @Override
    public void updateFilteredArchivedPersonList(Predicate<Person> predicate) {
        requireNonNull(predicate);
        filteredArchivedPersons.setPredicate(predicate);
    }

    //=========== Filtered Pin List Accessors ===============================================================

    /**
     * Returns an unmodifiable view of the list of {@code Person} backed by the internal list of
     * {@code versionedPinBook}
     */
    @Override
    public ObservableList<Person> getFilteredPinnedPersonList() {
        return filteredPinnedPersons;
    }

    @Override
    public void updateFilteredPinnedPersonList(Predicate<Person> predicate) {
        requireNonNull(predicate);
        filteredPinnedPersons.setPredicate(predicate);
    }

    //=========== Undo/Redo =================================================================================

    @Override
    public boolean canUndoAddressBook() {
        return versionedAddressBook.canUndo();
    }

    @Override
    public boolean canRedoAddressBook() {
        return versionedAddressBook.canRedo();
    }

    @Override
    public void undoAddressBook() {
        versionedAddressBook.undo();
    }

    @Override
    public void redoAddressBook() {
        versionedAddressBook.redo();
    }

    @Override
    public void commitAddressBook() {
        versionedAddressBook.commit();
    }

    @Override
    public void undoArchiveBook() {
        versionedArchiveBook.undo();
    }

    @Override
    public void redoArchiveBook() {
        versionedArchiveBook.redo();
    }

    @Override
    public void commitArchiveBook() {
        versionedArchiveBook.commit();
    }

    @Override
    public void undoPinBook() {
        versionedPinBook.undo();
    }

    @Override
    public void redoPinBook() {
        versionedPinBook.redo();
    }

    @Override
    public void commitPinBook() {
        versionedPinBook.commit();
    }

    //=========== Selected person ===========================================================================

    @Override
    public ReadOnlyProperty<Person> selectedPersonProperty() {
        return selectedPerson;
    }

    @Override
    public Person getSelectedPerson() {
        return selectedPerson.getValue();
    }

    @Override
    public void setSelectedPerson(Person person) {
        if (person != null && !filteredPersons.contains(person)) {
            throw new PersonNotFoundException();
        }
        selectedPerson.setValue(person);
    }

    /**
     * Ensures {@code selectedPerson} is a valid person in {@code filteredPersons}.
     */
    private void ensureSelectedPersonIsValid(ListChangeListener.Change<? extends Person> change) {
        while (change.next()) {
            if (selectedPerson.getValue() == null) {
                // null is always a valid selected person, so we do not need to check that it is valid anymore.
                return;
            }

            boolean wasSelectedPersonReplaced = change.wasReplaced() && change.getAddedSize() == change.getRemovedSize()
                    && change.getRemoved().contains(selectedPerson.getValue());
            if (wasSelectedPersonReplaced) {
                // Update selectedPerson to its new value.
                int index = change.getRemoved().indexOf(selectedPerson.getValue());
                selectedPerson.setValue(change.getAddedSubList().get(index));
                continue;
            }

            boolean wasSelectedPersonRemoved = change.getRemoved().stream()
                    .anyMatch(removedPerson -> selectedPerson.getValue().isSamePerson(removedPerson));
            if (wasSelectedPersonRemoved) {
                // Select the person that came before it in the list,
                // or clear the selection if there is no such person.
                selectedPerson.setValue(change.getFrom() > 0 ? change.getList().get(change.getFrom() - 1) : null);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        // short circuit if same object
        if (obj == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(obj instanceof ModelManager)) {
            return false;
        }

        // state check
        ModelManager other = (ModelManager) obj;
        return versionedAddressBook.equals(other.versionedAddressBook)
                && versionedArchiveBook.equals(other.versionedArchiveBook)
                && versionedPinBook.equals(other.versionedPinBook)
                && userPrefs.equals(other.userPrefs)
                && filteredPersons.equals(other.filteredPersons)
                && filteredArchivedPersons.equals(other.filteredArchivedPersons)
                && Objects.equals(selectedPerson.get(), other.selectedPerson.get());
    }

}
