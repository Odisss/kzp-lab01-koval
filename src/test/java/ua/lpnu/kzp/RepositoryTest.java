package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class RepositoryTest {
    private static final Medicine PARACETAMOL = Medicine.of("Парацетамол", "таблетки", 45.5, 420, false);
    private static final Medicine INSULIN = Medicine.of("Інсулін", "розчин", 520.0, 90, true);

    /* add і all зберігають порядок додавання. */
    @Test
    void addAndAllKeepOrder() {
        Repository<Medicine> repository = new Repository<>();
        repository.add(PARACETAMOL);
        repository.add(INSULIN);

        assertEquals(List.of(PARACETAMOL, INSULIN), repository.all());
        assertEquals(2, repository.size());
    }

    /* Знімок незмінний і не змінюється після нового add. */
    @Test
    void snapshotIsImmutableAndDetached() {
        Repository<Medicine> repository = new Repository<>();
        repository.add(PARACETAMOL);
        List<Medicine> snapshot = repository.all();

        repository.add(INSULIN);

        assertEquals(1, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(INSULIN));
        assertEquals(2, repository.all().size());
    }

    /* find приймає предикат супертипу (Predicate<? super T>). */
    @Test
    void findAcceptsSupertypePredicate() {
        Repository<Medicine> repository = new Repository<>();
        repository.addAll(List.of(PARACETAMOL, INSULIN));
        Predicate<Object> any = item -> true;

        assertEquals(List.of(INSULIN), repository.find(Medicine::prescription));
        assertEquals(2, repository.find(any).size());
        assertTrue(repository.find(medicine -> medicine.price() > 1000).isEmpty());
    }

    /* null не потрапляє до сховища. */
    @Test
    void nullIsRejected() {
        Repository<String> repository = new Repository<>();

        assertThrows(NullPointerException.class, () -> repository.add(null));
        assertThrows(NullPointerException.class, () -> repository.find(null));
        assertEquals(0, repository.size());
    }
}
