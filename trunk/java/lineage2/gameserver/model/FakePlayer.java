/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package lineage2.gameserver.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import lineage2.commons.threading.RunnableImpl;
import lineage2.gameserver.Config;
import lineage2.gameserver.ThreadPoolManager;
import lineage2.gameserver.ai.CtrlIntention;
import lineage2.gameserver.ai.FakePlayerAI;
import lineage2.gameserver.instancemanager.ReflectionManager;
import lineage2.gameserver.listener.actor.OnAttackListener;
import lineage2.gameserver.listener.actor.OnMagicUseListener;
import lineage2.gameserver.model.items.Inventory;
import lineage2.gameserver.model.items.ItemInstance;
import lineage2.gameserver.network.serverpackets.AutoAttackStart;
import lineage2.gameserver.network.serverpackets.CharInfo;
import lineage2.gameserver.network.serverpackets.L2GameServerPacket;
import lineage2.gameserver.network.serverpackets.RelationChanged;
import lineage2.gameserver.templates.item.WeaponTemplate;
import lineage2.gameserver.templates.player.PlayerTemplate;

/**
 * @author Mobius
 * @version $Revision: 1.0 $
 */
public class FakePlayer extends Playable
{
	/**
	 * Field serialVersionUID. (value is -7275714049223105460)
	 */
	private static final long serialVersionUID = -7275714049223105460L;
	/**
	 * Field _owner.
	 */
	private final Player _owner;
	/**
	 * Field _listener.
	 */
	private final OwnerAttakListener _listener;
	/**
	 * Field _broadcastCharInfoTask.
	 */
	ScheduledFuture<?> _broadcastCharInfoTask;
	
	/**
	 * Constructor for FakePlayer.
	 * @param objectId int
	 * @param template PlayerTemplate
	 * @param owner Player
	 */
	public FakePlayer(int objectId, PlayerTemplate template, Player owner)
	{
		super(objectId, template);
		_owner = owner;
		_ai = new FakePlayerAI(this);
		_listener = new OwnerAttakListener();
		owner.addListener(_listener);
	}
	
	/**
	 * Method getPlayer.
	 * @return Player
	 */
	@Override
	public Player getPlayer()
	{
		return _owner;
	}
	
	/**
	 * Method onSpawn.
	 */
	@Override
	protected void onSpawn()
	{
		super.onSpawn();
		getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, getPlayer(), Integer.valueOf(Config.FOLLOW_RANGE));
	}
	
	/**
	 * Method getAI.
	 * @return FakePlayerAI
	 */
	@Override
	public FakePlayerAI getAI()
	{
		return (FakePlayerAI) _ai;
	}
	
	/**
	 * Method isAutoAttackable.
	 * @param attacker Creature
	 * @return boolean
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return false;
	}
	
	/**
	 * Method getLevel.
	 * @return int
	 */
	@Override
	public int getLevel()
	{
		return _owner.getLevel();
	}
	
	/**
	 * Method getActiveWeaponInstance.
	 * @return ItemInstance
	 */
	@Override
	public ItemInstance getActiveWeaponInstance()
	{
		return _owner.getActiveWeaponInstance();
	}
	
	/**
	 * Method getActiveWeaponItem.
	 * @return WeaponTemplate
	 */
	@Override
	public WeaponTemplate getActiveWeaponItem()
	{
		return _owner.getActiveWeaponItem();
	}
	
	/**
	 * Method getSecondaryWeaponInstance.
	 * @return ItemInstance
	 */
	@Override
	public ItemInstance getSecondaryWeaponInstance()
	{
		return _owner.getSecondaryWeaponInstance();
	}
	
	/**
	 * Method getSecondaryWeaponItem.
	 * @return WeaponTemplate
	 */
	@Override
	public WeaponTemplate getSecondaryWeaponItem()
	{
		return _owner.getSecondaryWeaponItem();
	}
	
	/**
	 * Method broadcastCharInfo.
	 */
	@Override
	public void broadcastCharInfo()
	{
		if (_broadcastCharInfoTask != null)
		{
			return;
		}
		_broadcastCharInfoTask = ThreadPoolManager.getInstance().schedule(new BroadcastCharInfoTask(), Config.BROADCAST_CHAR_INFO_INTERVAL);
	}
	
	/**
	 * Method broadcastCharInfoImpl.
	 */
	public void broadcastCharInfoImpl()
	{
		for (Player player : World.getAroundPlayers(this))
		{
			player.sendPacket(new CharInfo(this));
		}
	}
	
	/**
	 * Method notifyOwerStartAttak.
	 */
	public void notifyOwerStartAttak()
	{
		getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, _owner.getTarget());
	}
	
	/**
	 * @author Mobius
	 */
	private class OwnerAttakListener implements OnAttackListener, OnMagicUseListener
	{
		/**
		 * Constructor for OwnerAttakListener.
		 */
		public OwnerAttakListener()
		{
			// TODO Auto-generated constructor stub
		}
		
		/**
		 * Method onMagicUse.
		 * @param actor Creature
		 * @param skill Skill
		 * @param target Creature
		 * @param alt boolean
		 * @see lineage2.gameserver.listener.actor.OnMagicUseListener#onMagicUse(Creature, Skill, Creature, boolean)
		 */
		@Override
		public void onMagicUse(Creature actor, Skill skill, Creature target, boolean alt)
		{
			notifyOwerStartAttak();
		}
		
		/**
		 * Method onAttack.
		 * @param actor Creature
		 * @param target Creature
		 * @see lineage2.gameserver.listener.actor.OnAttackListener#onAttack(Creature, Creature)
		 */
		@Override
		public void onAttack(Creature actor, Creature target)
		{
			notifyOwerStartAttak();
		}
	}
	
	/**
	 * @author Mobius
	 */
	public class BroadcastCharInfoTask extends RunnableImpl
	{
		/**
		 * Method runImpl.
		 */
		@Override
		public void runImpl()
		{
			broadcastCharInfoImpl();
			_broadcastCharInfoTask = null;
		}
	}
	
	/**
	 * Method addPacketList.
	 * @param forPlayer Player
	 * @param dropper Creature
	 * @return List<L2GameServerPacket>
	 */
	@Override
	public List<L2GameServerPacket> addPacketList(Player forPlayer, Creature dropper)
	{
		List<L2GameServerPacket> list = new ArrayList<>();
		Player owner = getPlayer();
		if (owner == forPlayer)
		{
			list.add(new CharInfo(this));
		}
		else
		{
			Party party = forPlayer.getParty();
			if ((getReflection() == ReflectionManager.GIRAN_HARBOR) && ((owner == null) || (party == null) || (party != owner.getParty())))
			{
				return list;
			}
			list.add(new CharInfo(this));
			list.add(RelationChanged.update(forPlayer, this, forPlayer));
		}
		if (isInCombat())
		{
			list.add(new AutoAttackStart(getObjectId()));
		}
		if (isMoving || isFollow)
		{
			list.add(movePacket());
		}
		return list;
	}

	@Override
	public Inventory getInventory()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getWearedMask()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void doPickupItem(GameObject object)
	{
		// TODO Auto-generated method stub
		
	}
	

}
