package org.edu_sharing.elasticsearch.edu_sharing.client;

public class Repository {

    String repositoryType, renderingSupported, id, title, icon, logo;
    Boolean isHomeRepo;

    public String getRepositoryType() {
        return repositoryType;
    }

    public void setRepositoryType(String repositoryType) {
        this.repositoryType = repositoryType;
    }

    public String getRenderingSupported() {
        return renderingSupported;
    }

    public void setRenderingSupported(String renderingSupported) {
        this.renderingSupported = renderingSupported;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public Boolean isHomeRepo() {
        return isHomeRepo;
    }

    public void setIsHomeRepo(Boolean ishomeRepo) {
        isHomeRepo = ishomeRepo;
    }
}
