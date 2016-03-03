
public class JNIPoC {
   static {
      System.loadLibrary("poc"); 
   }
 
   private native void sayHello();
}
