/*
 * Copyright (C) 2004-2016 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.effecthandlers;

import com.l2jserver.gameserver.enums.ShotType;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.conditions.Condition;
import com.l2jserver.gameserver.model.effects.AbstractEffect;
import com.l2jserver.gameserver.model.effects.L2EffectType;
import com.l2jserver.gameserver.model.skills.BuffInfo;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.model.stats.Formulas;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * Physical Soul Attack effect implementation.
 * @author Adry_85
 */
public final class PhysicalSoulAttack extends AbstractEffect
{
	public PhysicalSoulAttack(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean calcSuccess(BuffInfo info)
	{
		return !Formulas.calcPhysicalSkillEvasion(info.getEffector(), info.getEffected(), info.getSkill());
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.PHYSICAL_ATTACK;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		L2Character target = info.getEffected();
		L2Character activeChar = info.getEffector();
		Skill skill = info.getSkill();
		
		if (activeChar.isAlikeDead())
		{
			return;
		}
		
		if (((skill.getFlyRadius() > 0) || (skill.getFlyType() != null)) && activeChar.isMovementDisabled())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
			activeChar.sendPacket(sm);
			return;
		}
		
		if (target.isPlayer() && target.getActingPlayer().isFakeDeath())
		{
			target.stopFakeDeath(true);
		}
		
		int damage = 0;
		boolean ss = skill.isPhysical() && activeChar.isChargedShot(ShotType.SOULSHOTS);
		final byte shld = Formulas.calcShldUse(activeChar, target, skill);
		// Physical damage critical rate is only affected by STR.
		boolean crit = false;
		if (skill.getBaseCritRate() > 0)
		{
			crit = Formulas.calcCrit(activeChar, target, skill);
		}
		
		damage = (int) Formulas.calcPhysDam(activeChar, target, skill, shld, false, ss);
		
		if ((skill.getMaxSoulConsumeCount() > 0) && activeChar.isPlayer())
		{
			// Souls Formula (each soul increase +4%)
			damage *= 1 + (info.getCharges() * 0.04);
		}
		if (crit)
		{
			damage *= 2;
		}
		
		if (damage > 0)
		{
			activeChar.sendDamageMessage(target, damage, false, crit, false);
			target.reduceCurrentHp(damage, activeChar, skill);
			target.notifyDamageReceived(damage, activeChar, skill, crit, false, false);
			
			// Check if damage should be reflected
			Formulas.calcDamageReflected(activeChar, target, skill, crit);
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.ATTACK_FAILED);
		}
		
		if (skill.isSuicideAttack())
		{
			activeChar.doDie(activeChar);
		}
	}
}