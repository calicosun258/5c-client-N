package fifthcolumn.n.client.reflection;

import java.lang.reflect.Field;
import java.util.Iterator;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

public class FabricReflect {
   public static Field getField(Class<?> cls, String obfName, String deobfName) {
      if (cls == null) {
         return null;
      } else {
         Class<?> cls1 = cls;

         while(true) {
            Field field;
            if (cls1 != null) {
               try {
                  field = cls1.getDeclaredField(obfName);
               } catch (Exception var11) {
                  try {
                     field = cls1.getDeclaredField(deobfName);
                  } catch (Exception var10) {
                     cls1 = cls1.getSuperclass();
                     continue;
                  }
               }

               if (!field.isAccessible()) {
                  field.setAccessible(true);
               }

               return field;
            }

            Iterator var12 = ClassUtils.getAllInterfaces(cls).iterator();

            while(true) {
               if (var12.hasNext()) {
                  Class<?> class1 = (Class)var12.next();

                  try {
                     field = class1.getField(obfName);
                  } catch (Exception var9) {
                     try {
                        field = class1.getField(deobfName);
                     } catch (Exception var8) {
                        continue;
                     }
                  }

                  return field;
               }

               throw new RuntimeException("Error reflecting field: " + deobfName + "/" + obfName + " @" + cls.getSimpleName());
            }
         }
      }
   }

   public static Object getFieldValue(Object target, String obfName, String deobfName) {
      if (target == null) {
         return null;
      } else {
         Class<?> cls = target.getClass();
         Class<?> cls1 = cls;

         while(true) {
            Field field;
            if (cls1 != null) {
               try {
                  field = cls1.getDeclaredField(obfName);
               } catch (Exception var14) {
                  try {
                     field = cls1.getDeclaredField(deobfName);
                  } catch (Exception var13) {
                     cls1 = cls1.getSuperclass();
                     continue;
                  }
               }

               if (!field.isAccessible()) {
                  field.setAccessible(true);
               }

               try {
                  return field.get(target);
               } catch (Exception var9) {
                  throw new RuntimeException("Error getting reflected field value: " + deobfName + "/" + obfName + " @" + target.getClass().getSimpleName());
               }
            }

            Iterator var15 = ClassUtils.getAllInterfaces(cls).iterator();

            while(true) {
               if (var15.hasNext()) {
                  Class<?> class1 = (Class)var15.next();

                  try {
                     field = class1.getField(obfName);
                  } catch (Exception var12) {
                     try {
                        field = class1.getField(deobfName);
                     } catch (Exception var11) {
                        continue;
                     }
                  }

                  try {
                     return field.get(target);
                  } catch (Exception var10) {
                     throw new RuntimeException("Error getting reflected field value: " + deobfName + "/" + obfName + " @" + target.getClass().getSimpleName());
                  }
               }

               throw new RuntimeException("Error getting reflected field value: " + deobfName + "/" + obfName + " @" + target.getClass().getSimpleName());
            }
         }
      }
   }

   public static void writeField(Object target, Object value, String obfName, String deobfName) {
      if (target != null) {
         Class<?> cls = target.getClass();
         Field field = getField(cls, obfName, deobfName);
         if (!field.isAccessible()) {
            field.setAccessible(true);
         }

         try {
            field.set(target, value);
         } catch (Exception var7) {
            throw new RuntimeException("Error writing reflected field: " + deobfName + "/" + obfName + " @" + target.getClass().getSimpleName());
         }
      }
   }

   public static Object invokeMethod(Object target, String obfName, String deobfName, Object... args) {
      Object o;
      try {
         o = MethodUtils.invokeMethod(target, true, obfName, args);
      } catch (Exception var8) {
         try {
            o = MethodUtils.invokeMethod(target, true, deobfName, args);
         } catch (Exception var7) {
            throw new RuntimeException("Error reflecting method: " + deobfName + "/" + obfName + " @" + target.getClass().getSimpleName());
         }
      }

      return o;
   }
}
