package com.kukelekuuk.fortuneswitcher.Mixins;

import com.kukelekuuk.fortuneswitcher.FortuneSwitcher;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.ListTag;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RayTraceContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin( MinecraftClient.class )
public class ToolSwitcherMixin
{

    @Inject( method = "tick", slice = @Slice( from = @At( value = "INVOKE", target = "Lnet/minecraft/util/profiler/DisableableProfiler;push(Ljava/lang/String;)V" ) ), at = @At( value = "HEAD", ordinal = 0 ) )
    private void clientTick( CallbackInfo callbackInfo )
    {
        tickClient();
    }

    @Inject( method = "handleInputEvents", at = @At( "HEAD" ), cancellable = true )
    private void handleInput( CallbackInfo callbackInfo )
    {
        if ( FortuneSwitcher.keyBinding.wasPressed() )
        {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            player.world.playSound( player, player.getBlockPos(), SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 1f, FortuneSwitcher.isEnabled ? .5f : 1f );
            FortuneSwitcher.isEnabled = !FortuneSwitcher.isEnabled;
        }
    }

    private void tickClient()
    {
        if ( !FortuneSwitcher.isEnabled )
        {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        ClientPlayerEntity player = client.player;
        if ( world != null && player != null )
        {
            BlockHitResult result = rayTrace( player, client.interactionManager.getReachDistance() );
            if ( result.getType() == HitResult.Type.BLOCK )
            {
                BlockState block = world.getBlockState( result.getBlockPos() );
                String name = block.getBlock().getTranslationKey();
                name = name.replaceAll( "^.*\\.([a-zA-Z_]+)$", "$1" );
                if ( isOre( name ) )
                {
                    swapToFortune( player );
                }
                else if ( isShovelable( name ) )
                {
                    swapToShovel( player );
                }
                else
                {
                    swapToSilk( player );
                }
            }
        }
    }

    private BlockHitResult rayTrace( ClientPlayerEntity entity, double playerReach )
    {
        Vec3d eyePosition = entity.getCameraPosVec( (float) 0 );
        Vec3d lookVector = entity.getRotationVec( (float) 0 );
        Vec3d traceEnd = eyePosition.add( lookVector.x * playerReach, lookVector.y * playerReach, lookVector.z * playerReach );

        return entity.getEntityWorld().rayTrace( new RayTraceContext( eyePosition, traceEnd, RayTraceContext.ShapeType.COLLIDER, RayTraceContext.FluidHandling.NONE, entity ) );
    }

    private void swapToFortune( ClientPlayerEntity player )
    {
        swapToEnchant( player, "minecraft:fortune" );
    }

    private void swapToShovel( ClientPlayerEntity player )
    {
        int rightSlot = -1;
        for ( int i = 0; i < 9; i++ )
        {
            if ( player.inventory.main.get( i ).getTranslationKey().toLowerCase().contains( "shovel" ) )
            {
                rightSlot = i;
                break;
            }
        }
        if ( rightSlot < 0 )
        {
            return;
        }
        player.inventory.selectedSlot = rightSlot;
    }

    private void swapToSilk( ClientPlayerEntity player )
    {
        swapToEnchant( player, "minecraft:silk_touch" );
    }

    private void swapToEnchant( ClientPlayerEntity player, String enchant )
    {
        int rightSlot = -1;
        for ( int i = 0; i < 9; i++ )
        {
            boolean done = false;
            if ( player.inventory.main.get( i ).getTranslationKey().toLowerCase().contains( "pickaxe" ) && player.inventory.main.get( i ).hasEnchantments() )
            {
                ListTag enchants = player.inventory.main.get( i ).getEnchantments();
                for ( int n = 0; n < enchants.size(); n++ )
                {
                    String enchantname = enchants.getCompoundTag( n ).getString( "id" );
                    if ( enchantname.equalsIgnoreCase( enchant ) )
                    {
                        rightSlot = i;
                        done = true;
                        break;
                    }

                }
            }
            if ( done )
            {
                break;
            }
        }
        if ( rightSlot < 0 )
        {
            return;
        }
        player.inventory.selectedSlot = rightSlot;
    }

    private boolean isOre( String name )
    {
        if ( name.equalsIgnoreCase( "coal_ore" ) ||
                name.equalsIgnoreCase( "redstone_ore" ) ||
                name.equalsIgnoreCase( "lapis_ore" ) ||
                name.equalsIgnoreCase( "emerald_ore" ) ||
                name.equalsIgnoreCase( "diamond_ore" ) )
        {
            return true;
        }
        return false;
    }

    private boolean isShovelable( String name )
    {
        if ( name.equalsIgnoreCase( "dirt" ) ||
                name.equalsIgnoreCase( "grass_block" ) ||
                name.equalsIgnoreCase( "gravel" ) ||
                name.equalsIgnoreCase( "sand" ) )
        {
            return true;
        }
        return false;
    }
}
