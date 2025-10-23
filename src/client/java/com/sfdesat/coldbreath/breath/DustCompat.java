package com.sfdesat.coldbreath.breath;

import net.minecraft.particle.DustParticleEffect;

import java.lang.reflect.Constructor;

/**
 * Compatibility shim for DustParticleEffect across Minecraft versions.
 * 
 * This class provides a unified interface for creating DustParticleEffect instances
 * that works across different Minecraft versions:
 * 
 * - 1.21.2+: new DustParticleEffect(int rgb, float scale)
 * - 1.20.x/early 1.21: new DustParticleEffect(float r, float g, float b, float scale)
 * - 1.21.1: new DustParticleEffect(org.joml.Vector3f color, float scale)
 * - (rare) older mappings: new DustParticleEffect(com.mojang.math.Vector3f color, float scale)
 * 
 * The shim uses reflection to detect which constructor is available at runtime
 * and caches the result for performance. It automatically converts between
 * the different formats as needed.
 */
public final class DustCompat {
    
    private enum ConstructorType {
        INT_FLOAT,           // (int, float) - 1.21.2+
        FLOAT_FLOAT_FLOAT,   // (float, float, float, float) - 1.20.x/early 1.21
        JOML_VECTOR,         // (org.joml.Vector3f, float) - 1.21.1
        MOJANG_VECTOR        // (com.mojang.math.Vector3f, float) - older mappings
    }
    
    private static ConstructorType constructorType;
    private static Constructor<DustParticleEffect> constructor;
    private static boolean initialized = false;
    
    private DustCompat() {}
    
    /**
     * Creates a DustParticleEffect compatible with all Minecraft versions
     * @param rgb RGB color as integer (0xRRGGBB)
     * @param scale Particle scale (clamped to 0.01f-4.0f)
     * @return DustParticleEffect instance
     */
    public static DustParticleEffect make(int rgb, float scale) {
        if (!initialized) {
            initializeConstructors();
        }
        
        // Clamp scale to Minecraft's bounds
        float clampedScale = Math.max(0.01f, Math.min(4.0f, scale));
        
        try {
            float r = ((rgb >> 16) & 0xFF) / 255f;
            float g = ((rgb >>  8) & 0xFF) / 255f;
            float b = ( rgb        & 0xFF) / 255f;
            
            switch (constructorType) {
                case INT_FLOAT:
                    return constructor.newInstance(rgb, clampedScale);
                    
                case FLOAT_FLOAT_FLOAT:
                    return constructor.newInstance(r, g, b, clampedScale);
                    
                case JOML_VECTOR:
                case MOJANG_VECTOR:
                    // Create Vector3f instance using reflection
                    Object vector = createVector3f(r, g, b);
                    return constructor.newInstance(vector, clampedScale);
                    
                default:
                    throw new RuntimeException("No compatible DustParticleEffect constructor found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DustParticleEffect", e);
        }
    }
    
    /**
     * Creates a DustParticleEffect with individual RGB components
     * @param r Red component (0.0-1.0)
     * @param g Green component (0.0-1.0)
     * @param b Blue component (0.0-1.0)
     * @param scale Particle scale (clamped to 0.01f-4.0f)
     * @return DustParticleEffect instance
     */
    public static DustParticleEffect make(float r, float g, float b, float scale) {
        if (!initialized) {
            initializeConstructors();
        }
        
        // Clamp scale to Minecraft's bounds
        float clampedScale = Math.max(0.01f, Math.min(4.0f, scale));
        
        try {
            switch (constructorType) {
                case INT_FLOAT:
                    // Convert to int RGB
                    int rgb = ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
                    return constructor.newInstance(rgb, clampedScale);
                    
                case FLOAT_FLOAT_FLOAT:
                    return constructor.newInstance(r, g, b, clampedScale);
                    
                case JOML_VECTOR:
                case MOJANG_VECTOR:
                    // Create Vector3f instance using reflection
                    Object vector = createVector3f(r, g, b);
                    return constructor.newInstance(vector, clampedScale);
                    
                default:
                    throw new RuntimeException("No compatible DustParticleEffect constructor found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DustParticleEffect", e);
        }
    }
    
    private static void initializeConstructors() {
        try {
            // Try constructors in order of preference
            
            // 1. Try int constructor (1.21.2+)
            try {
                constructor = DustParticleEffect.class.getConstructor(int.class, float.class);
                constructorType = ConstructorType.INT_FLOAT;
                initialized = true;
                return;
            } catch (NoSuchMethodException e) {
                // Not available, try next
            }
            
            // 2. Try float constructor (1.20.x/early 1.21)
            try {
                constructor = DustParticleEffect.class.getConstructor(float.class, float.class, float.class, float.class);
                constructorType = ConstructorType.FLOAT_FLOAT_FLOAT;
                initialized = true;
                return;
            } catch (NoSuchMethodException e) {
                // Not available, try next
            }
            
            // 3. Try JOML Vector3f constructor (1.21.1)
            try {
                Class<?> jomlVectorClass = Class.forName("org.joml.Vector3f");
                constructor = DustParticleEffect.class.getConstructor(jomlVectorClass, float.class);
                constructorType = ConstructorType.JOML_VECTOR;
                initialized = true;
                return;
            } catch (NoSuchMethodException | ClassNotFoundException e) {
                // Not available, try next
            }
            
            // 4. Try Mojang Vector3f constructor (older mappings)
            try {
                Class<?> mojangVectorClass = Class.forName("com.mojang.math.Vector3f");
                constructor = DustParticleEffect.class.getConstructor(mojangVectorClass, float.class);
                constructorType = ConstructorType.MOJANG_VECTOR;
                initialized = true;
                return;
            } catch (NoSuchMethodException | ClassNotFoundException e) {
                // Not available
            }
            
            throw new RuntimeException("No compatible DustParticleEffect constructors found");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize DustParticleEffect constructors", e);
        }
    }
    
    /**
     * Creates a Vector3f instance using reflection
     */
    private static Object createVector3f(float x, float y, float z) throws Exception {
        if (constructorType == ConstructorType.JOML_VECTOR) {
            Class<?> jomlVectorClass = Class.forName("org.joml.Vector3f");
            return jomlVectorClass.getConstructor(float.class, float.class, float.class).newInstance(x, y, z);
        } else if (constructorType == ConstructorType.MOJANG_VECTOR) {
            Class<?> mojangVectorClass = Class.forName("com.mojang.math.Vector3f");
            return mojangVectorClass.getConstructor(float.class, float.class, float.class).newInstance(x, y, z);
        } else {
            throw new RuntimeException("Vector3f creation not supported for constructor type: " + constructorType);
        }
    }
}
