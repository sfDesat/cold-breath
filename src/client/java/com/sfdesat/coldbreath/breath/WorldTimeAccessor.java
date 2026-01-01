package com.sfdesat.coldbreath.breath;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * Version-compatible accessor for ClientWorld.getTime() method.
 * 
 * This class uses reflection to access the getTime() method which may have
 * been removed or renamed in Minecraft 1.21.11+.
 * 
 * The accessor tries multiple approaches:
 * 1. Direct getTime() method (pre-1.21.11)
 * 2. Reflection-based method lookup
 * 3. Fallback to alternative methods if available
 */
public final class WorldTimeAccessor {
    
    private static final Logger LOGGER = LogManager.getLogger("ColdBreath/WorldTimeAccessor");
    
    private enum AccessType {
        REFLECTION_METHOD,   // Using reflection Method
        REFLECTION_HANDLE,   // Using MethodHandle
        FALLBACK             // Using alternative approach (getTimeOfDay)
    }
    
    private static AccessType accessType;
    private static MethodHandle methodHandle;
    private static Method reflectionMethod;
    private static boolean initialized = false;
    private static boolean loggedWarning = false;
    
    private WorldTimeAccessor() {}
    
    /**
     * Gets the world time using a version-compatible approach.
     * @param world The ClientWorld instance
     * @return The world time in ticks, or 0 if unavailable
     */
    public static long getTime(ClientWorld world) {
        if (world == null) {
            return 0L;
        }
        
        if (!initialized) {
            initialize();
        }
        
        try {
            switch (accessType) {
                case REFLECTION_METHOD:
                    if (reflectionMethod != null) {
                        return (long) reflectionMethod.invoke(world);
                    }
                    break;
                    
                case REFLECTION_HANDLE:
                    if (methodHandle != null) {
                        return (long) methodHandle.invoke(world);
                    }
                    break;
                    
                case FALLBACK:
                    // Try to use getTimeOfDay() as a fallback (less accurate but better than nothing)
                    // Note: This only gives day time, not total world time
                    long dayTime = world.getTimeOfDay();
                    if (!loggedWarning) {
                        LOGGER.warn("Using getTimeOfDay() as fallback for getTime() - scheduling may be less accurate");
                        loggedWarning = true;
                    }
                    return dayTime;
            }
        } catch (Throwable e) {
            if (!loggedWarning) {
                LOGGER.error("Failed to get world time: {}", e.toString());
                loggedWarning = true;
            }
        }
        
        return 0L;
    }
    
    private static void initialize() {
        try {
            // 1. Try to find getTime() on ClientWorld using reflection with MethodHandle (fastest)
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                methodHandle = lookup.findVirtual(ClientWorld.class, "getTime",
                        MethodType.methodType(long.class));
                accessType = AccessType.REFLECTION_HANDLE;
                initialized = true;
                LOGGER.debug("Using MethodHandle for ClientWorld.getTime()");
                return;
            } catch (NoSuchMethodException | IllegalAccessException e) {
                // Not available, try next
            }
            
            // 2. Try to find getTime() on World class (parent of ClientWorld)
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                methodHandle = lookup.findVirtual(World.class, "getTime",
                        MethodType.methodType(long.class));
                accessType = AccessType.REFLECTION_HANDLE;
                initialized = true;
                LOGGER.debug("Using MethodHandle for World.getTime()");
                return;
            } catch (NoSuchMethodException | IllegalAccessException e) {
                // Not available, try next
            }
            
            // 3. Try to find getTime() using reflection with Method on ClientWorld
            try {
                reflectionMethod = ClientWorld.class.getMethod("getTime");
                reflectionMethod.setAccessible(true);
                accessType = AccessType.REFLECTION_METHOD;
                initialized = true;
                LOGGER.debug("Using Method reflection for ClientWorld.getTime()");
                return;
            } catch (NoSuchMethodException e) {
                // Not available, try next
            }
            
            // 4. Try to find getTime() using reflection with Method on World
            try {
                reflectionMethod = World.class.getMethod("getTime");
                reflectionMethod.setAccessible(true);
                accessType = AccessType.REFLECTION_METHOD;
                initialized = true;
                LOGGER.debug("Using Method reflection for World.getTime()");
                return;
            } catch (NoSuchMethodException e) {
                // Not available, try next
            }
            
            // 5. Try to find method by searching all methods (in case it was renamed)
            try {
                Method[] methods = ClientWorld.class.getMethods();
                for (Method m : methods) {
                    if (m.getReturnType() == long.class && 
                        m.getParameterCount() == 0 &&
                        (m.getName().equals("getTime") || 
                         m.getName().equals("getWorldTime") ||
                         m.getName().equals("getTotalTime"))) {
                        reflectionMethod = m;
                        reflectionMethod.setAccessible(true);
                        accessType = AccessType.REFLECTION_METHOD;
                        initialized = true;
                        LOGGER.info("Found alternative time method on ClientWorld: {}", m.getName());
                        return;
                    }
                }
                // Also check World class
                methods = World.class.getMethods();
                for (Method m : methods) {
                    if (m.getReturnType() == long.class && 
                        m.getParameterCount() == 0 &&
                        (m.getName().equals("getTime") || 
                         m.getName().equals("getWorldTime") ||
                         m.getName().equals("getTotalTime"))) {
                        reflectionMethod = m;
                        reflectionMethod.setAccessible(true);
                        accessType = AccessType.REFLECTION_METHOD;
                        initialized = true;
                        LOGGER.info("Found alternative time method on World: {}", m.getName());
                        return;
                    }
                }
            } catch (Exception e) {
                // Continue to fallback
            }
            
            // 6. Fallback: use getTimeOfDay() (less accurate)
            accessType = AccessType.FALLBACK;
            initialized = true;
            LOGGER.warn("getTime() method not found on ClientWorld or World, using getTimeOfDay() as fallback");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize WorldTimeAccessor: {}", e.toString(), e);
            // Set fallback as last resort
            accessType = AccessType.FALLBACK;
            initialized = true;
        }
    }
    
    /**
     * Gets information about the access method being used (for debugging).
     * @return String describing the access type
     */
    public static String getAccessInfo() {
        if (!initialized) {
            return "not initialized";
        }
        return accessType.name().toLowerCase().replace("_", " ");
    }
}

