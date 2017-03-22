package org.exoplatform.addon.service.model;

import javax.jcr.Value;

public class Addon {
  private String uuid;
  private String jcrNodePath;
  private String seeDetailUrl;
  private String name;
  private String description;
  private String downloadLink;
  private String author;
  private Double voteRate;
  private Integer totalVote;
  private String coverImagePath;
  private String ownerid;
  private String category;
  
  public String getUuid() {
    return uuid;
  }
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }
  
  public String getJcrNodePath() {
    return jcrNodePath;
  }
  public void setJcrNodePath(String jcrNodePath) {
    this.jcrNodePath = jcrNodePath;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }
  public String getDownloadLink() {
    return downloadLink;
  }
  public void setDownloadLink(String downloadLink) {
    this.downloadLink = downloadLink;
  }
  public String getAuthor() {
    return author;
  }
  public void setAuthor(String author) {
    this.author = author;
  }
  public Double getVoteRate() {
    return voteRate;
  }
  public void setVoteRate(Double voteRate) {
    this.voteRate = voteRate;
  }
  public Integer getTotalVote() {
    return totalVote;
  }
  public void setTotalVote(Integer totalVote) {
    this.totalVote = totalVote;
  }
  public String getCoverImagePath() {
    return coverImagePath;
  }
  public void setCoverImagePath(String coverImagePath) {
    this.coverImagePath = coverImagePath;
  }
  public String getOwnerid() {
    return ownerid;
  }
  public void setOwnerid(String ownerid) {
    this.ownerid = ownerid;
  }
  public String getSeeDetailUrl() {
    return seeDetailUrl;
  }
  public void setSeeDetailUrl(String seeDetailUrl) {
    this.seeDetailUrl = seeDetailUrl;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }
}
