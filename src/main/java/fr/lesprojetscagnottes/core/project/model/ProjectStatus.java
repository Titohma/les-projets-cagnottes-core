package fr.lesprojetscagnottes.core.project.model;

public enum ProjectStatus {
    NEW("NEW"),
    DRAFT("DRAFT"),
    IDEA("IDEA"),
    IN_PROGRESS("IN_PROGRESS"),
    ON_PAUSE("ON_PAUSE"),
    FINISHED("FINISHED");

    public final String label;

    ProjectStatus(String label) {
        this.label = label;
    }
}
