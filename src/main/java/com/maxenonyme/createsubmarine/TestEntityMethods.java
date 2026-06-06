public class TestEntityMethods {
    public static void main(String[] args) throws Exception {
        for (java.lang.reflect.Method m : net.minecraft.world.entity.Entity.class.getDeclaredMethods()) {
            if (m.getName().toLowerCase().contains("fluid") || m.getName().toLowerCase().contains("water") || m.getName().toLowerCase().contains("eye")) {
                System.out.println(m.getReturnType().getSimpleName() + " " + m.getName() + "(...)");
            }
        }
    }
}
