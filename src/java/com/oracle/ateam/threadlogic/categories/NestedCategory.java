package com.oracle.ateam.threadlogic.categories;

import java.util.HashMap;
import java.util.Iterator;

import com.oracle.ateam.threadlogic.filter.Filter;
import com.oracle.ateam.threadlogic.utils.IconFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NestedCategory extends CustomCategory {

  private NestedCategory parent = null;

  private HashMap<String, NestedCategory> subCategories;

  public NestedCategory(String name) {
    super(name);
  }

  public void addFilter(Filter filter, CustomCategory cat) {
    cat.addToFilters(filter);
  }

  public void addToFilters(Filter filter) {
    addFilterAndCreateSubCategory(filter);
  }

  public void addFilterAndCreateSubCategory(Filter filter) {
    super.addToFilters(filter);

    // Each filter can also be a subcategory
    if (subCategories == null) {
      subCategories = new HashMap<String, NestedCategory>();
    }

    NestedCategory subCat = new NestedCategory(filter.getName());
    subCat.setParent(this);

    String filterNameLower = filter.getName().toLowerCase();
    Pattern problematicFilterPattern = Pattern.compile("(blocked)|(warning)|(stuck)|(hogging)");
    Matcher m = problematicFilterPattern.matcher(filterNameLower);
    if (m.find())
      subCat.setAsBlockedIcon();

    subCategories.put(filter.getName(), subCat);
  }

  public NestedCategory getSubCategory(String categoryName) {
    return subCategories != null ? subCategories.get(categoryName) : null;
  }

  public Iterator<NestedCategory> getSubCategoriesIterator() {
    return subCategories != null ? subCategories.values().iterator() : null;
  }

  public NestedCategory getParent() {
    return parent;
  }

  public void setParent(NestedCategory parent) {
    this.parent = parent;
  }

  public void setAsBlockedIcon() {
    this.setIconID(IconFactory.BLOCKEDTHREAD_CATEGORY);
  }

}
