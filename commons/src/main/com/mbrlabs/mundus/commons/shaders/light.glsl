// Lighting adapted from OGLDEV tutorials https://www.youtube.com/c/OGLDEV
#ifdef GL_ES
#define LOW lowp
#define MED mediump
#define HIGH highp
precision highp float;
#else
#define MED
#define LOW
#define HIGH
#endif

struct BaseLight
{
    MED vec3 Color;
    MED vec3 AmbientColor;
    MED float AmbientIntensity;
    MED float DiffuseIntensity;
};

struct DirectionalLight
{
    BaseLight Base;
    MED vec3 Direction;
};

struct Attenuation
{
    float Constant;
    float Linear;
    float Exp;
};

struct PointLight
{
    BaseLight Base;
    MED vec3 LocalPos;
    Attenuation Atten;
};

struct SpotLight
{
    PointLight Base;
    MED vec3 Direction;
    MED float Cutoff;
};

varying vec3 v_worldPos;

uniform int u_useSpecular;
uniform int u_activeNumPointLights;
uniform int u_activeNumSpotLights;
uniform vec4 u_cameraPosition;
uniform MED float u_shininess;
uniform DirectionalLight u_directionalLight;
uniform PointLight u_pointLights[numPointLights];
uniform SpotLight u_spotLights[numSpotLights];

uniform sampler2D u_shadowTexture;
uniform float u_shadowPCFOffset;
uniform float u_shadowBias;
uniform int u_useShadows;
varying vec3 v_shadowMapUv;

float getShadowness(vec2 offset)
{
    const vec4 bitShifts = vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 16581375.0);
    return step(v_shadowMapUv.z, dot(texture2D(u_shadowTexture, v_shadowMapUv.xy + offset), bitShifts) + u_shadowBias) /*+(.5/255.0)*/;
}

float getShadow()
{
    return (//getShadowness(vec2(0,0)) +
    getShadowness(vec2(u_shadowPCFOffset, u_shadowPCFOffset)) +
    getShadowness(vec2(-u_shadowPCFOffset, u_shadowPCFOffset)) +
    getShadowness(vec2(u_shadowPCFOffset, -u_shadowPCFOffset)) +
    getShadowness(vec2(-u_shadowPCFOffset, -u_shadowPCFOffset))) * 0.25;
}

vec4 CalcLightInternal(BaseLight Light, vec3 LightDirection, vec3 Normal)
{
    vec4 AmbientColor = vec4(Light.AmbientColor, 1.0) * Light.AmbientIntensity;

    float DiffuseFactor = dot(Normal, -LightDirection);

    MED vec4 DiffuseColor = vec4(0, 0, 0, 0);
    MED vec4 SpecularColor = vec4(0, 0, 0, 0);

    if (DiffuseFactor > 0.0) {
        DiffuseColor = vec4(Light.Color, 1.0) * Light.DiffuseIntensity * DiffuseFactor;

        if (u_useShadows == 1) {
            DiffuseColor *= getShadow();
        }

        if (u_useSpecular == 1) {
            vec3 PixelToCamera = normalize(u_cameraPosition.xyz - v_worldPos);
            vec3 LightReflect = normalize(reflect(LightDirection, Normal));
            float SpecularFactor = dot(PixelToCamera, LightReflect);
            if (SpecularFactor > 0.0) {
                // This is for specular map textures, which we may want later but not right now.
                //float SpecularExponent = texture2D(gSamplerSpecularExponent, TexCoord0).r * 255.0;
                float SpecularExponent = u_shininess;
                SpecularFactor = pow(SpecularFactor, SpecularExponent);
                SpecularColor = vec4(Light.Color, 1.0) *
                Light.DiffuseIntensity *// using the diffuse intensity for diffuse/specular
                SpecularFactor;
            }
        }
    }

    return (AmbientColor + DiffuseColor + SpecularColor);
}

vec4 CalcPointLight(PointLight l, vec3 Normal)
{
    vec3 LightDirection = v_worldPos - l.LocalPos;
    float dist = length(LightDirection);
    LightDirection = normalize(LightDirection);

    vec4 Color = CalcLightInternal(l.Base, LightDirection, Normal);
    float attenuation =  l.Atten.Constant +
    l.Atten.Linear * dist +
    l.Atten.Exp * dist * dist;

    return Color / attenuation;
}

vec4 CalcSpotLight(SpotLight l, vec3 Normal)
{
    vec3 LightToPixel = normalize(v_worldPos - l.Base.LocalPos);
    float SpotFactor = dot(LightToPixel, l.Direction);

    if (SpotFactor > l.Cutoff) {
        vec4 Color = CalcPointLight(l.Base, Normal);
        float SpotLightIntensity = (1.0 - (1.0 - SpotFactor)/(1.0 - l.Cutoff));
        return Color * SpotLightIntensity;
    }
    else {
        return vec4(0,0,0,0);
    }
}

vec4 CalcDirectionalLight(vec3 Normal)
{
    return CalcLightInternal(u_directionalLight.Base, u_directionalLight.Direction, Normal);
}
