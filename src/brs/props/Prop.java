package brs.props;

public class Prop<T> {

  String name;
  T defaultValue;

  public Prop(String name, T defaultValue) {
    this.name = name;
    this.defaultValue = defaultValue;
  }

  public String getName() {
    return name;
  }
}
