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
package lineage2.gameserver.network.serverpackets;

import java.io.PrintStream;
import java.util.Arrays;

import lineage2.gameserver.model.Creature;
import lineage2.gameserver.model.Player;
import lineage2.gameserver.model.Skill;
import lineage2.gameserver.model.Summon;
import lineage2.gameserver.model.instances.DoorInstance;
import lineage2.gameserver.model.instances.NpcInstance;
import lineage2.gameserver.model.items.ItemInstance;
import lineage2.gameserver.network.serverpackets.components.SystemMessageId;
import lineage2.gameserver.templates.item.ItemTemplate;
import lineage2.gameserver.templates.npc.NpcTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author UnAfraid
 * @param <T>
 */
@SuppressWarnings("unchecked")
public abstract class AbstractMessagePacket<T extends AbstractMessagePacket<?>> extends L2GameServerPacket
{
	private static final Logger _log = LoggerFactory.getLogger(AbstractMessagePacket.class);
	private static final SMParam[] EMPTY_PARAM_ARRAY = new SMParam[0];
	
	private static final class SMParam
	{
		private final byte _type;
		private final Object _value;
		
		public SMParam(final byte type, final Object value)
		{
			_type = type;
			_value = value;
		}
		
		public final byte getType()
		{
			return _type;
		}
		
		public final String getStringValue()
		{
			return (String) _value;
		}
		
		public final int getIntValue()
		{
			return ((Integer) _value).intValue();
		}
		
		public final long getLongValue()
		{
			return ((Long) _value).longValue();
		}
		
		public final int[] getIntArrayValue()
		{
			return (int[]) _value;
		}
	}
	
	private static final byte TYPE_POPUP_ID = 16;
	private static final byte TYPE_CLASS_ID = 15;
	// id 14 unknown
	private static final byte TYPE_SYSTEM_STRING = 13;
	private static final byte TYPE_PLAYER_NAME = 12;
	private static final byte TYPE_DOOR_NAME = 11;
	private static final byte TYPE_INSTANCE_NAME = 10;
	private static final byte TYPE_ELEMENT_NAME = 9;
	// id 8 - same as 3
	private static final byte TYPE_ZONE_NAME = 7;
	private static final byte TYPE_LONG_NUMBER = 6;
	private static final byte TYPE_CASTLE_NAME = 5;
	private static final byte TYPE_SKILL_NAME = 4;
	private static final byte TYPE_ITEM_NAME = 3;
	private static final byte TYPE_NPC_NAME = 2;
	private static final byte TYPE_INT_NUMBER = 1;
	private static final byte TYPE_TEXT = 0;
	
	private SMParam[] _params;
	private final SystemMessageId _smId;
	private int _paramIndex;
	
	public AbstractMessagePacket(SystemMessageId smId)
	{
		if (smId == null)
		{
			throw new NullPointerException("SystemMessageId cannot be null!");
		}
		_smId = smId;
		_params = smId.getParamCount() > 0 ? new SMParam[smId.getParamCount()] : EMPTY_PARAM_ARRAY;
	}
	
	public final int getId()
	{
		return _smId.getId();
	}
	
	public final SystemMessageId getSystemMessageId()
	{
		return _smId;
	}
	
	private final void append(SMParam param)
	{
		if (_paramIndex >= _params.length)
		{
			_params = Arrays.copyOf(_params, _paramIndex + 1);
			_smId.setParamCount(_paramIndex + 1);
			_log.info("Wrong parameter count '" + (_paramIndex + 1) + "' for SystemMessageId: " + _smId);
		}
		
		_params[_paramIndex++] = param;
	}
	
	public final T addString(final String text)
	{
		append(new SMParam(TYPE_TEXT, text));
		return (T) this;
	}
	
	/**
	 * Appends a Castle name parameter type, the name will be read from CastleName-e.dat.<br>
	 * <ul>
	 * <li>1-9 Castle names</li>
	 * <li>21 Fortress of Resistance</li>
	 * <li>22-33 Clan Hall names</li>
	 * <li>34 Devastated Castle</li>
	 * <li>35 Bandit Stronghold</li>
	 * <li>36-61 Clan Hall names</li>
	 * <li>62 Rainbow Springs</li>
	 * <li>63 Wild Beast Reserve</li>
	 * <li>64 Fortress of the Dead</li>
	 * <li>81-89 Territory names</li>
	 * <li>90-100 null</li>
	 * <li>101-121 Fortress names</li>
	 * </ul>
	 * @param number the conquerable entity
	 * @return the system message with the proper parameter
	 */
	public final T addCastleId(final int number)
	{
		append(new SMParam(TYPE_CASTLE_NAME, number));
		return (T) this;
	}
	
	public final T addInt(final int number)
	{
		append(new SMParam(TYPE_INT_NUMBER, number));
		return (T) this;
	}
	
	public final T addLong(final long number)
	{
		append(new SMParam(TYPE_LONG_NUMBER, number));
		return (T) this;
	}
	
	public final T addCharName(final Creature cha)
	{
		if (cha.isNpc())
		{
			final NpcInstance npc = (NpcInstance) cha;
			if (npc.getTemplate().isShowName())
			{
				return addString(npc.getTemplate().getName());
			}
			return addNpcName(npc);
		}
		else if (cha.isPlayer())
		{
			return addPcName(cha.getPlayer());
		}
		else if (cha.isSummon())
		{
			final Summon summon = (Summon) cha;
			if (summon.getTemplate().isShowName())
			{
				return addString(summon.getTemplate().getName());
			}
			return addNpcName(summon);
		}
		else if (cha.isDoor())
		{
			final DoorInstance door = (DoorInstance) cha;
			return addDoorName(door.getId());
		}
		return addString(cha.getName());
	}
	
	public final T addPcName(final Player pc)
	{
		append(new SMParam(TYPE_PLAYER_NAME, pc.getName()));
		return (T) this;
	}
	
	/**
	 * ID from doorData.xml
	 * @param doorId
	 * @return
	 */
	public final T addDoorName(int doorId)
	{
		append(new SMParam(TYPE_DOOR_NAME, doorId));
		return (T) this;
	}
	
	public final T addNpcName(NpcInstance npc)
	{
		return addNpcName(npc.getTemplate());
	}
	
	public final T addNpcName(final Summon npc)
	{
		return addNpcName(npc.getId());
	}
	
	public final T addNpcName(final NpcTemplate template)
	{
		if (template.isShowName())
		{
			return addString(template.getName());
		}
		return addNpcName(template.getId());
	}
	
	public final T addNpcName(final int id)
	{
		append(new SMParam(TYPE_NPC_NAME, 1000000 + id));
		return (T) this;
	}
	
	public T addItemName(final ItemInstance item)
	{
		return addItemName(item.getId());
	}
	
	public T addItemName(final ItemTemplate item)
	{
		return addItemName(item.getId());
	}
	
	public final T addItemName(final int id)
	{
		// TODO: Add support for server-side custom items
		// final ItemTemplate item = ItemHolder.getInstance().getTemplate(id);
		// if (item.getDisplayId() != id)
		// {
		// return addString(item.getName());
		// }
		
		append(new SMParam(TYPE_ITEM_NAME, id));
		return (T) this;
	}
	
	public final T addZoneName(final int x, final int y, final int z)
	{
		append(new SMParam(TYPE_ZONE_NAME, new int[]
		{
			x,
			y,
			z
		}));
		return (T) this;
	}
	
	public final T addSkillName(final Skill skill)
	{
		if (skill.getId() != skill.getDisplayId())
		{
			return addString(skill.getName());
		}
		return addSkillName(skill.getId(), skill.getLevel());
	}
	
	public final T addSkillName(final int id)
	{
		return addSkillName(id, 1);
	}
	
	public final T addSkillName(final int id, final int lvl)
	{
		append(new SMParam(TYPE_SKILL_NAME, new int[]
		{
			id,
			lvl
		}));
		return (T) this;
	}
	
	/**
	 * Elemental name - 0(Fire) ...
	 * @param type
	 * @return
	 */
	public final T addElemental(final int type)
	{
		append(new SMParam(TYPE_ELEMENT_NAME, type));
		return (T) this;
	}
	
	/**
	 * ID from sysstring-e.dat
	 * @param type
	 * @return
	 */
	public final T addSystemString(final int type)
	{
		append(new SMParam(TYPE_SYSTEM_STRING, type));
		return (T) this;
	}
	
	/**
	 * ID from ClassInfo-e.dat
	 * @param type
	 * @return
	 */
	public final T addClassId(final int type)
	{
		append(new SMParam(TYPE_CLASS_ID, type));
		return (T) this;
	}
	
	public final T addPopup(int target, int attacker, int damage)
	{
		append(new SMParam(TYPE_POPUP_ID, new int[]
		{
			target,
			attacker,
			damage
		}));
		return (T) this;
	}
	
	/**
	 * Instance name from instantzonedata-e.dat
	 * @param type id of instance
	 * @return
	 */
	public final T addInstanceName(final int type)
	{
		append(new SMParam(TYPE_INSTANCE_NAME, type));
		return (T) this;
	}
	
	protected void writeParamsSize(int size)
	{
		writeC(size);
	}
	
	protected void writeParamType(int type)
	{
		writeC(type);
	}
	
	protected final void writeMe()
	{
		writeParamsSize(_params.length);
		SMParam param;
		for (int i = 0; i < _paramIndex; i++)
		{
			param = _params[i];
			
			writeParamType(param.getType());
			switch (param.getType())
			{
				case TYPE_TEXT:
				case TYPE_PLAYER_NAME:
				{
					writeS(param.getStringValue());
					break;
				}
				
				case TYPE_LONG_NUMBER:
				{
					writeQ(param.getLongValue());
					break;
				}
				
				case TYPE_ITEM_NAME:
				case TYPE_CASTLE_NAME:
				case TYPE_INT_NUMBER:
				case TYPE_NPC_NAME:
				case TYPE_ELEMENT_NAME:
				case TYPE_SYSTEM_STRING:
				case TYPE_INSTANCE_NAME:
				case TYPE_DOOR_NAME:
				{
					writeD(param.getIntValue());
					break;
				}
				
				case TYPE_SKILL_NAME:
				{
					final int[] array = param.getIntArrayValue();
					writeD(array[0]); // SkillId
					writeH(array[1]); // SkillLevel
					break;
				}
				
				case TYPE_POPUP_ID:
				case TYPE_ZONE_NAME:
				{
					final int[] array = param.getIntArrayValue();
					writeD(array[0]); // x
					writeD(array[1]); // y
					writeD(array[2]); // z
					break;
				}
				case TYPE_CLASS_ID:
				{
					writeH(param.getIntValue());
					break;
				}
			}
		}
	}
	
	public final void printMe(PrintStream out)
	{
		out.println(0x62);
		
		out.println(getId());
		out.println(_params.length);
		
		for (SMParam param : _params)
		{
			switch (param.getType())
			{
				case TYPE_TEXT:
				case TYPE_PLAYER_NAME:
				{
					out.println(param.getStringValue());
					break;
				}
				
				case TYPE_LONG_NUMBER:
				{
					out.println(param.getLongValue());
					break;
				}
				
				case TYPE_ITEM_NAME:
				case TYPE_CASTLE_NAME:
				case TYPE_INT_NUMBER:
				case TYPE_NPC_NAME:
				case TYPE_ELEMENT_NAME:
				case TYPE_SYSTEM_STRING:
				case TYPE_INSTANCE_NAME:
				case TYPE_DOOR_NAME:
				case TYPE_CLASS_ID:
				{
					out.println(param.getIntValue());
					break;
				}
				
				case TYPE_POPUP_ID:
				{
					final int[] array = param.getIntArrayValue();
					out.println(array[0]); // Target
					out.println(array[1]); // Attacker
					out.println(array[2]); // Value
					break;
				}
				
				case TYPE_SKILL_NAME:
				{
					final int[] array = param.getIntArrayValue();
					out.println(array[0]); // SkillId
					out.println(array[1]); // SkillLevel
					break;
				}
				
				case TYPE_ZONE_NAME:
				{
					final int[] array = param.getIntArrayValue();
					out.println(array[0]); // x
					out.println(array[1]); // y
					out.println(array[2]); // z
					break;
				}
			}
		}
	}
}
