package l1j.server.server.model;

import l1j.server.Config;
import l1j.server.server.ActionCodes;
import l1j.server.server.WarTimeController;
import l1j.server.server.datatables.SkillsTable;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1PetInstance;
import l1j.server.server.model.Instance.L1SummonInstance;
import l1j.server.server.model.skill.L1NamedSkill;
import l1j.server.server.model.skill.L1SkillName;
import l1j.server.server.serverpackets.S_DoActionGFX;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_SkillSound;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.templates.L1MagicDoll;
import l1j.server.server.templates.L1Skills;
import l1j.server.server.utils.Random;

import static l1j.server.server.model.skill.L1SkillId.*;

public class L1Magic {
    private int _calcType;

    private final int PC_PC = 1;

    private final int PC_NPC = 2;

    private final int NPC_PC = 3;

    private final int NPC_NPC = 4;

    private L1Character _target = null;

    private L1PcInstance _pc = null;

    private L1PcInstance _targetPc = null;

    private L1NpcInstance _npc = null;

    private L1NpcInstance _targetNpc = null;

    private int _leverage = 10;

    public void setLeverage(int i) {
        _leverage = i;
    }

    private int getLeverage() {
        return _leverage;
    }

    public L1Magic(L1Character attacker, L1Character target) {
        _target = target;

        if (attacker instanceof L1PcInstance) {
            if (target instanceof L1PcInstance) {
                _calcType = PC_PC;
                _pc = (L1PcInstance) attacker;
                _targetPc = (L1PcInstance) target;
            }
            else {
                _calcType = PC_NPC;
                _pc = (L1PcInstance) attacker;
                _targetNpc = (L1NpcInstance) target;
            }
        }
        else {
            if (target instanceof L1PcInstance) {
                _calcType = NPC_PC;
                _npc = (L1NpcInstance) attacker;
                _targetPc = (L1PcInstance) target;
            }
            else {
                _calcType = NPC_NPC;
                _npc = (L1NpcInstance) attacker;
                _targetNpc = (L1NpcInstance) target;
            }
        }
    }

    private int getMagicLevel() {
        int magicLevel = 0;
        if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
            magicLevel = _pc.getMagicLevel();
        }
        else if ((_calcType == NPC_PC) || (_calcType == NPC_NPC)) {
            magicLevel = _npc.getMagicLevel();
        }
        return magicLevel;
    }

    private int getMagicBonus() {
        int magicBonus = 0;
        if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
            magicBonus = _pc.getMagicBonus();
        }
        else if ((_calcType == NPC_PC) || (_calcType == NPC_NPC)) {
            magicBonus = _npc.getMagicBonus();
        }
        return magicBonus;
    }

    private int getLawful() {
        int lawful = 0;
        if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
            lawful = _pc.getLawful();
        }
        else if ((_calcType == NPC_PC) || (_calcType == NPC_NPC)) {
            lawful = _npc.getLawful();
        }
        return lawful;
    }

    private int getTargetMr() {
        int mr = 0;
        if ((_calcType == PC_PC) || (_calcType == NPC_PC)) {
            mr = _targetPc.getMr();
        }
        else {
            mr = _targetNpc.getMr();
        }
        return mr;
    }


    public boolean calcProbabilityMagic(int skillId) {
        int probability = 0;
        boolean isSuccess = false;

        if ((_pc != null) && _pc.isGm()) {
            return true;
        }

        if ((_calcType == PC_NPC) && (_targetNpc != null)) {
            if (_pc.isAttackMiss(_pc, _targetNpc.getNpcTemplate().get_npcId())) {
                return false;
            }
        }

        if (!checkZone(skillId)) {
            return false;
        }
        if (skillId == CANCELLATION) {
            if ((_calcType == PC_PC) && (_pc != null) && (_targetPc != null)) {
                if (_pc.getId() == _targetPc.getId()) {
                    return true;
                }
                if ((_pc.getClanid() > 0) && (_pc.getClanid() == _targetPc.getClanid())) {
                    return true;
                }
                if (_pc.isInParty()) {
                    if (_pc.getParty().isMember(_targetPc)) {
                        return true;
                    }
                }
                if ((_pc.getZoneType() == 1) || (_targetPc.getZoneType() == 1)) {
                    return false;
                }
            }
            if ((_calcType == PC_NPC) || (_calcType == NPC_PC) || (_calcType == NPC_NPC)) {
                return true;
            }
        }

        if ((_calcType == PC_PC) || (_calcType == NPC_PC)) {
            if (_targetPc.hasSkillEffect(EARTH_BIND)) {
                if ((skillId != WEAPON_BREAK) && (skillId != CANCELLATION)) {
                    return false;
                }
            }
        }
        else {
            if (_targetNpc.hasSkillEffect(EARTH_BIND)) {
                if ((skillId != WEAPON_BREAK) && (skillId != CANCELLATION)) {
                    return false;
                }
            }
        }

        probability = calcProbability(skillId);

        int rnd = Random.nextInt(100) + 1;
        if (probability > 90) {
            probability = 90;
        }

        if (probability >= rnd) {
            isSuccess = true;
        }
        else {
            isSuccess = false;
        }

        if (!Config.ALT_ATKMSG) {
            return isSuccess;
        }
        if (Config.ALT_ATKMSG) {
            if (((_calcType == PC_PC) || (_calcType == PC_NPC)) && !_pc.isGm()) {
                return isSuccess;
            }
            if (((_calcType == PC_PC) || (_calcType == NPC_PC)) && !_targetPc.isGm()) {
                return isSuccess;
            }
        }

        String msg0 = "";
        String msg1 = " Ã¦-Â½Ã¦â€�Â¾Ã©Â­â€�Ã¦Â³â€¢ ";
        String msg2 = "";
        String msg3 = "";
        String msg4 = "";

        if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
            msg0 = _pc.getName() + " Ã¥Â°ï¿½";
        }
        else if (_calcType == NPC_PC) {
            msg0 = _npc.getName();
        }

        msg2 = "Ã¯Â¼Å’Ã¦Â©Å¸Ã§Å½â€¡Ã¯Â¼Å¡" + probability + "%";
        if ((_calcType == NPC_PC) || (_calcType == PC_PC)) {
            msg4 = _targetPc.getName();
        }
        else if (_calcType == PC_NPC) {
            msg4 = _targetNpc.getName();
        }
        if (isSuccess == true) {
            msg3 = "Ã¦Ë†ï¿½Ã¥Å Å¸";
        }
        else {
            msg3 = "Ã¥Â¤Â±Ã¦â€¢-";
        }

        if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
            _pc.sendPackets(new S_ServerMessage(166, msg0, msg1, msg2, msg3, msg4));
        }
        else if ((_calcType == NPC_PC)) {
            _targetPc.sendPackets(new S_ServerMessage(166, msg0, msg1, msg2, msg3, null));
        }

        return isSuccess;
    }

    private boolean checkZone(int skillId) {
        if ((_pc != null) && (_targetPc != null)) {
            if ((_pc.getZoneType() == 1) || (_targetPc.getZoneType() == 1)) {
                if ((skillId == WEAPON_BREAK) || (skillId == SLOW) || (skillId == CURSE_PARALYZE) || (skillId == MANA_DRAIN) || (skillId == DARKNESS)
                        || (skillId == WEAKNESS) || (skillId == DISEASE) || (skillId == DECAY_POTION) || (skillId == MASS_SLOW)
                        || (skillId == ENTANGLE) || (skillId == ERASE_MAGIC) || (skillId == EARTH_BIND) || (skillId == AREA_OF_SILENCE)
                        || (skillId == WIND_SHACKLE) || (skillId == STRIKER_GALE) || (skillId == SHOCK_STUN) || (skillId == FOG_OF_SLEEPING)
                        || (skillId == ICE_LANCE) || (skillId == FREEZING_BLIZZARD) || (skillId == FREEZING_BREATH) || (skillId == POLLUTE_WATER)
                        || (skillId == ELEMENTAL_FALL_DOWN) || (skillId == RETURN_TO_NATURE)
                        || (skillId == ICE_LANCE_COCKATRICE) || (skillId == ICE_LANCE_BASILISK) || (skillId == SILENCE)) {
                    return false;
                }
            }
        }
        return true;
    }

    private int calcProbability(int skillId) {
        L1Skills l1skills = SkillsTable.getInstance().getTemplate(skillId);
        int attackLevel = 0;
        int defenseLevel = 0;
        int probability = 0;

        if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
            attackLevel = _pc.getLevel();
        }
        else {
            attackLevel = _npc.getLevel();
        }

        if ((_calcType == PC_PC) || (_calcType == NPC_PC)) {
            defenseLevel = _targetPc.getLevel();
        }
        else {
            defenseLevel = _targetNpc.getLevel();
            if (skillId == RETURN_TO_NATURE) {
                if (_targetNpc instanceof L1SummonInstance) {
                    L1SummonInstance summon = (L1SummonInstance) _targetNpc;
                    defenseLevel = summon.getMaster().getLevel();
                }
            }
        }

        if ((skillId == ELEMENTAL_FALL_DOWN) || (skillId == RETURN_TO_NATURE) || (skillId == ENTANGLE) || (skillId == ERASE_MAGIC)
                || (skillId == AREA_OF_SILENCE) || (skillId == WIND_SHACKLE) || (skillId == STRIKER_GALE) || (skillId == POLLUTE_WATER)) {
            probability = (int) (((l1skills.getProbabilityDice()) / 10D) * (attackLevel - defenseLevel)) + l1skills.getProbabilityValue();

            if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
                probability += 2 * _pc.getOriginalMagicHit();
            }
        }




        else if (skillId == COUNTER_BARRIER) {
            int bonus = Math.max(0, (attackLevel - 60) / 4);

            probability = l1skills.getProbabilityValue() + bonus;

            if (_calcType == PC_PC || _calcType == PC_NPC) {
                probability += 2 * _pc.getOriginalMagicHit();
            }
        }


        else if (skillId == GUARD_BRAKE || skillId == RESIST_FEAR
                || skillId == HORROR_OF_DEATH) {
            probability = 50 + (attackLevel - defenseLevel) * 3;
            if (skillId == GUARD_BRAKE) probability -= 3;
            if (skillId == RESIST_FEAR) probability -= 5;

            if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
                probability += 2 * _pc.getOriginalMagicHit();
            }
        }

        else if(skillId == ARMOR_BREAK)
        {
            probability = 30 + (attackLevel - defenseLevel) * 2;
        }
        else if (skillId == THUNDER_GRAB) {
            probability = 50
                    * (attackLevel / Math.max(1, defenseLevel))
                    - Random.nextInt(21);

            if (_calcType == PC_PC || _calcType == PC_NPC) {
                probability += 2 * _pc.getOriginalMagicHit();
            }
        }
        else if (skillId == SHOCK_STUN) {
            if(attackLevel+20 < defenseLevel)
            {
                probability = 1;
            }
            else
            {
                probability = 40 + (attackLevel - defenseLevel)*2;
            }
        }
        else if (skillId == BONE_BREAK) {
            if(attackLevel+20 < defenseLevel)
            {
                probability = 1;
            }
            else
            {
                probability = 15 + (attackLevel - defenseLevel)*2;
            }
        }
        else if (skillId == EARTH_BIND) {
            if(attackLevel+20 < defenseLevel)
            {
                probability = 1;
            }
            else
            {
                probability = 30 + (attackLevel - defenseLevel)*2;
            }
        }
        else if (skillId == PHANTASM || skillId == FOG_OF_SLEEPING) {
            if(attackLevel+20 < defenseLevel)
            {
                probability = 1;
            }
            else
            {
                probability = 50 + (attackLevel - defenseLevel)*2;
            }
        }
        else if (skillId == CURSE_PARALYZE) {
            if(attackLevel+20 < defenseLevel)
            {
                probability = 1;
            }
            else
            {
                if(_target.getMr() > 100)
                {
                    probability = 10 + (attackLevel - defenseLevel);
                }
                else
                {
                    probability = 20 + (attackLevel - defenseLevel);
                }

            }
        }
        else {
            int dice = l1skills.getProbabilityDice();
            int diceCount = 0;
            if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
                if (_pc.isWizard()) {
                    diceCount = getMagicBonus() + getMagicLevel() + 1;
                }
                else if (_pc.isElf()) {
                    diceCount = getMagicBonus() + getMagicLevel() - 1;
                }
                else {
                    diceCount = getMagicBonus() + getMagicLevel() - 1;
                }
            }
            else {
                diceCount = getMagicBonus() + getMagicLevel();
            }
            if (diceCount < 1) {
                diceCount = 1;
            }

            for (int i = 0; i < diceCount; i++) {
                probability += (Random.nextInt(dice) + 1);
            }
            probability = probability * getLeverage() / 10;

            if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
                probability += 2 * _pc.getOriginalMagicHit();
            }

            probability -= getTargetMr();

            if (skillId == TAMING_MONSTER) {
                double probabilityRevision = 1;
                if ((_targetNpc.getMaxHp() * 1 / 4) > _targetNpc.getCurrentHp()) {
                    probabilityRevision = 1.3;
                }
                else if ((_targetNpc.getMaxHp() * 2 / 4) > _targetNpc.getCurrentHp()) {
                    probabilityRevision = 1.2;
                }
                else if ((_targetNpc.getMaxHp() * 3 / 4) > _targetNpc.getCurrentHp()) {
                    probabilityRevision = 1.1;
                }
                probability *= probabilityRevision;
            }
        }
        if ((skillId == ICE_LANCE) || (skillId == FREEZING_BLIZZARD) || (skillId == FREEZING_BREATH)
                || (skillId == ICE_LANCE_COCKATRICE) || (skillId == ICE_LANCE_BASILISK)) {
            if ((_calcType == PC_PC) || (_calcType == NPC_PC)) {
                for (int skillid : INVINCIBLE) {
                    if (_targetPc.hasSkillEffect(skillid)) {
                        probability = 0;
                        break;
                    }
                }
            }
        }
        else if ((skillId == CURSE_BLIND) || (skillId == DARKNESS) || (skillId == DARK_BLIND)) {
            if ((_calcType == PC_PC) || (_calcType == NPC_PC)) {
            }
        }

        return probability;
    }

    private static final int[] INVINCIBLE = {
            ABSOLUTE_BARRIER, ICE_LANCE, FREEZING_BLIZZARD, FREEZING_BREATH, EARTH_BIND, ICE_LANCE_COCKATRICE, ICE_LANCE_BASILISK
    };



    public int calcMagicDamage(int skillId) {
        int damage = 0;

        for (int skillid : INVINCIBLE) {
            if (_target.hasSkillEffect(skillid)) {
                return damage;
            }
        }

        if ((_calcType == PC_PC) || (_calcType == NPC_PC)) {
            damage = calcPcMagicDamage(skillId);
        }
        else if ((_calcType == PC_NPC) || (_calcType == NPC_NPC)) {
            damage = calcNpcMagicDamage(skillId);
        }

        if (skillId != JOY_OF_PAIN) {
            damage = calcMrDefense(damage);
        }

        if (_calcType == PC_NPC && _pc.getDmgMessages()) {
            _pc.sendPackets(new S_SystemMessage(L1NamedSkill.getName(skillId) +
                    " Dealt:" + String.valueOf(damage)));
        }

        return damage;
    }

    public int calcPcFireWallDamage() {
        int dmg = 0;
        double attrDeffence = calcAttrResistance(L1Skills.ATTR_FIRE);
        L1Skills l1skills = SkillsTable.getInstance().getTemplate(FIRE_WALL);
        dmg = (int) ((1.0 - attrDeffence) * l1skills.getDamageValue());

        if (_targetPc.hasSkillEffect(ABSOLUTE_BARRIER)) {
            dmg = 0;
        }
        if (_targetPc.hasSkillEffect(ICE_LANCE)) {
            dmg = 0;
        }
        if (_targetPc.hasSkillEffect(FREEZING_BLIZZARD)) {
            dmg = 0;
        }
        if (_targetPc.hasSkillEffect(FREEZING_BREATH)) {
            dmg = 0;
        }
        if (_targetPc.hasSkillEffect(EARTH_BIND)) {
            dmg = 0;
        }
        if (_targetPc.hasSkillEffect(ICE_LANCE_COCKATRICE)) {
            dmg = 0;
        }
        if (_targetPc.hasSkillEffect(ICE_LANCE_BASILISK)) {
            dmg = 0;
        }

        if (dmg < 0) {
            dmg = 0;
        }

        return dmg;
    }

    public int calcNpcFireWallDamage() {
        int dmg = 0;
        double attrDeffence = calcAttrResistance(L1Skills.ATTR_FIRE);
        L1Skills l1skills = SkillsTable.getInstance().getTemplate(FIRE_WALL);
        dmg = (int) ((1.0 - attrDeffence) * l1skills.getDamageValue());

        if (_targetNpc.hasSkillEffect(ICE_LANCE)) {
            dmg = 0;
        }
        if (_targetNpc.hasSkillEffect(FREEZING_BLIZZARD)) {
            dmg = 0;
        }
        if (_targetNpc.hasSkillEffect(FREEZING_BREATH)) {
            dmg = 0;
        }
        if (_targetNpc.hasSkillEffect(EARTH_BIND)) {
            dmg = 0;
        }
        if (_targetNpc.hasSkillEffect(ICE_LANCE_COCKATRICE)) {
            dmg = 0;
        }
        if (_targetNpc.hasSkillEffect(ICE_LANCE_BASILISK)) {
            dmg = 0;
        }

        if (dmg < 0) {
            dmg = 0;
        }

        return dmg;
    }

    private int calcPcMagicDamage(int skillId) {
        int dmg = 0;

        dmg = calcMagicDiceDamage(skillId);
        dmg = (dmg * getLeverage()) / 10;


        if (skillId == MIND_BREAK) {
            if (_targetPc.getCurrentMp() >= 10) {
                _targetPc.setCurrentMp(_targetPc.getCurrentMp() - 10);
                if (_calcType == PC_PC) {
                    dmg += _pc.getWis() * 5;
                } else if (_calcType == NPC_PC) {
                    dmg += _npc.getWis() * 5;
                }
            }
        }

        dmg -= _targetPc.getDamageReductionByArmor();

        dmg -= L1MagicDoll.getDamageReductionByDoll(_targetPc);

        if (_targetPc.hasSkillEffect(COOKING_1_0_S)
                || _targetPc.hasSkillEffect(COOKING_1_1_S) || _targetPc.hasSkillEffect(COOKING_1_2_S)
                || _targetPc.hasSkillEffect(COOKING_1_3_S)
                || _targetPc.hasSkillEffect(COOKING_1_4_S) || _targetPc.hasSkillEffect(COOKING_1_5_S)
                || _targetPc.hasSkillEffect(COOKING_1_6_S)
                || _targetPc.hasSkillEffect(COOKING_2_0_S) || _targetPc.hasSkillEffect(COOKING_2_1_S)
                || _targetPc.hasSkillEffect(COOKING_2_2_S)
                || _targetPc.hasSkillEffect(COOKING_2_3_S) || _targetPc.hasSkillEffect(COOKING_2_4_S)
                || _targetPc.hasSkillEffect(COOKING_2_5_S)
                || _targetPc.hasSkillEffect(COOKING_2_6_S) || _targetPc.hasSkillEffect(COOKING_3_0_S)
                || _targetPc.hasSkillEffect(COOKING_3_1_S)
                || _targetPc.hasSkillEffect(COOKING_3_2_S) || _targetPc.hasSkillEffect(COOKING_3_3_S)
                || _targetPc.hasSkillEffect(COOKING_3_4_S)
                || _targetPc.hasSkillEffect(COOKING_3_5_S) || _targetPc.hasSkillEffect(COOKING_3_6_S)) {
            dmg -= 5;
        }
        if (_targetPc.hasSkillEffect(COOKING_1_7_S)
                || _targetPc.hasSkillEffect(COOKING_2_7_S) || _targetPc.hasSkillEffect(COOKING_3_7_S)) {
            dmg -= 5;
        }

        if (_targetPc.hasSkillEffect(REDUCTION_ARMOR)) {
            int targetPcLvl = _targetPc.getLevel();
            if (targetPcLvl < 50) {
                targetPcLvl = 50;
            }
            dmg -= (targetPcLvl - 50) / 5 + 1;
        }
        if (_targetPc.hasSkillEffect(DRAGON_SKIN)) {
            dmg -= 5;
        }

        if (_targetPc.hasSkillEffect(PATIENCE)) {
            dmg -= 2;
        }

        if (_calcType == NPC_PC) {
            boolean isNowWar = false;
            int castleId = L1CastleLocation.getCastleIdByArea(_targetPc);
            if (castleId > 0) {
                isNowWar = WarTimeController.getInstance().isNowWar(castleId);
            }
            if (!isNowWar) {
                if (_npc instanceof L1PetInstance) {
                    dmg /= 16;
                }
                if (_npc instanceof L1SummonInstance) {
                    L1SummonInstance summon = (L1SummonInstance) _npc;
                    if (summon.isExsistMaster()) {
                        dmg /= 16;
                    }
                }
            }
        }

        if (_targetPc.hasSkillEffect(IMMUNE_TO_HARM)) {
            dmg /= 2;
        }
        if (skillId == JOY_OF_PAIN) {
            int nowDamage = 0;
            if (_calcType == PC_PC) {
                nowDamage = _pc.getMaxHp() - _pc.getCurrentHp();
                if (nowDamage > 0) {
                    dmg = nowDamage / 5;
                }
            } else if (_calcType == NPC_PC) {
                nowDamage = _npc.getMaxHp() - _npc.getCurrentHp();
                if (nowDamage > 0) {
                    dmg = nowDamage / 5;
                }
            }
        }

        if (_targetPc.hasSkillEffect(ABSOLUTE_BARRIER)) {
            dmg = 0;
        } else if (_targetPc.hasSkillEffect(ICE_LANCE)) {
            dmg = 0;
        } else if (_targetPc.hasSkillEffect(FREEZING_BLIZZARD)) {
            dmg = 0;
        } else if (_targetPc.hasSkillEffect(FREEZING_BREATH)) {
            dmg = 0;
        } else if (_targetPc.hasSkillEffect(EARTH_BIND)) {
            dmg = 0;
        }

        if (L1MagicDoll.getDamageEvasionByDoll(_targetPc) > 0) {
            dmg = 0;
        }

        if (_calcType == NPC_PC) {
            if ((_npc instanceof L1PetInstance) || (_npc instanceof L1SummonInstance)) {
                if ((_targetPc.getZoneType() == 1) || (_npc.getZoneType() == 1)
                        || (_targetPc.checkNonPvP(_targetPc, _npc))) {
                    dmg = 0;
                }
            }
        }

        if (_targetPc.hasSkillEffect(COUNTER_MIRROR)) {
            if (_calcType == PC_PC) {
                if (_targetPc.getWis() >= Random.nextInt(100)) {
                    _pc.sendPackets(new S_DoActionGFX(_pc.getId(), ActionCodes.ACTION_Damage));
                    _pc.broadcastPacket(new S_DoActionGFX(_pc.getId(), ActionCodes.ACTION_Damage));
                    _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 4395));
                    _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 4395));
                    _pc.receiveDamage(_targetPc, dmg, false);
                    dmg = 0;
                    _targetPc.killSkillEffectTimer(COUNTER_MIRROR);
                }
            }
            else if (_calcType == NPC_PC) {
                int npcId = _npc.getNpcTemplate().get_npcId();
                if ((npcId == 45681) || (npcId == 45682) || (npcId == 45683) || (npcId == 45684)) {}
                else if (!_npc.getNpcTemplate().get_IsErase()) {}
                else {
                    if (_targetPc.getWis() >= Random.nextInt(100)) {
                        _npc.broadcastPacket(new S_DoActionGFX(_npc.getId(), ActionCodes.ACTION_Damage));
                        _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 4395));
                        _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 4395));
                        _npc.receiveDamage(_targetPc, dmg);
                        dmg = 0;
                        _targetPc.killSkillEffectTimer(COUNTER_MIRROR);
                    }
                }
            }
        }

        if (dmg < 0) {
            dmg = 0;
        }
        if(_targetPc instanceof  L1PcInstance)
        {
            if(_targetPc.isGm())
            {
                _targetPc.sendPackets(new S_SystemMessage("Magic Dmg Recieved: " + dmg));
            }
        }
        return dmg;
    }

    private int calcNpcMagicDamage(int skillId) {
        int dmg = 0;


        dmg = calcMagicDiceDamage(skillId);
        dmg = (dmg * getLeverage()) / 10;


        if (skillId == MIND_BREAK) {
            if (_targetNpc.getCurrentMp() >= 10) {
                _targetNpc.setCurrentMp(_targetNpc.getCurrentMp() - 10);
                if (_calcType == PC_NPC) {
                    dmg += _pc.getWis() * 5;
                } else if (_calcType == NPC_NPC) {
                    dmg += _npc.getWis() * 5;
                }
            }
        }

        if (skillId == JOY_OF_PAIN) {
            int nowDamage = 0;
            if (_calcType == PC_NPC) {
                nowDamage = _pc.getMaxHp() - _pc.getCurrentHp();
                if (nowDamage > 0) {
                    dmg = nowDamage / 5;
                }
            } else if (_calcType == NPC_NPC) {
                nowDamage = _npc.getMaxHp() - _npc.getCurrentHp();
                if (nowDamage > 0) {
                    dmg = nowDamage / 5;
                }
            }
        }

        if (_calcType == PC_NPC) {
            boolean isNowWar = false;
            int castleId = L1CastleLocation.getCastleIdByArea(_targetNpc);
            if (castleId > 0) {
                isNowWar = WarTimeController.getInstance().isNowWar(castleId);
            }
            if (!isNowWar) {
                if (_targetNpc instanceof L1PetInstance) {
                    dmg /= 8;
                }
                if (_targetNpc instanceof L1SummonInstance) {
                    L1SummonInstance summon = (L1SummonInstance) _targetNpc;
                    if (summon.isExsistMaster()) {
                        dmg /= 8;
                    }
                }
            }
        }

        if (_targetNpc.hasSkillEffect(ICE_LANCE)) {
            dmg = 0;
        } else if (_targetNpc.hasSkillEffect(FREEZING_BLIZZARD)) {
            dmg = 0;
        } else if (_targetNpc.hasSkillEffect(FREEZING_BREATH)) {
            dmg = 0;
        } else if (_targetNpc.hasSkillEffect(EARTH_BIND)) {
            dmg = 0;
        }

        if ((_calcType == PC_NPC) && (_targetNpc != null)) {
            if (_pc.isAttackMiss(_pc, _targetNpc.getNpcTemplate().get_npcId())) {
                dmg = 0;
            }
        }
        if (_calcType == NPC_NPC) {
            if (((_npc instanceof L1PetInstance) || (_npc instanceof L1SummonInstance))
                    && ((_targetNpc instanceof L1PetInstance) || (_targetNpc instanceof L1SummonInstance))) {
                if ((_targetNpc.getZoneType() == 1) || (_npc.getZoneType() == 1)) {
                    dmg = 0;
                }
                if (_targetNpc.getMaster() == _npc.getMaster()) {
                    dmg = 0;
                }
            }
        }

        if(_targetPc instanceof  L1PcInstance)
        {
            if(_targetPc.isGm())
            {
                _targetPc.sendPackets(new S_SystemMessage("Magic Dmg Recieved: " + dmg));
            }
        }
        return dmg;
    }

    private int calcMagicDiceDamage(int skillId) {
        L1Skills l1skills = SkillsTable.getInstance().getTemplate(skillId);
        int dice = l1skills.getDamageDice();
        int diceCount = l1skills.getDamageDiceCount();
        int value = l1skills.getDamageValue();
        int magicDamage = 0;
        int charaIntelligence = 0;

        for (int i = 0; i < diceCount; i++) {
            magicDamage += (Random.nextInt(dice) + 1);
        }
        magicDamage += value;

        if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
            int weaponAddDmg = 0;
            L1ItemInstance weapon = _pc.getWeapon();
            if (weapon != null) {
                weaponAddDmg = weapon.getItem().getMagicDmgModifier();
            }
            magicDamage += weaponAddDmg;
        }

        if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
            int spByItem = _pc.getSp() - _pc.getTrueSp();
            charaIntelligence = _pc.getInt() + spByItem - 12;
        }
        else if ((_calcType == NPC_PC) || (_calcType == NPC_NPC)) {
            int spByItem = _npc.getSp() - _npc.getTrueSp();
            charaIntelligence = _npc.getInt() + spByItem - 12;
        }
        if (charaIntelligence < 1) {
            charaIntelligence = 1;
        }

        double attrDeffence = calcAttrResistance(l1skills.getAttr());

        double coefficient = (1.0 - attrDeffence + charaIntelligence * 3.0 / 32.0);
        if (coefficient < 0) {
            coefficient = 0;
        }

        magicDamage *= coefficient;

        double criticalCoefficient = 1.5;
        int rnd = Random.nextInt(100) + 1;
        if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
            if (l1skills.getSkillLevel() <= 6) {
                if (rnd <= (10 + _pc.getOriginalMagicCritical())) {
                    magicDamage *= criticalCoefficient;
                }
            }
        }

        if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
            magicDamage += _pc.getOriginalMagicDamage();
        }
        if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
            if (_pc.hasSkillEffect(ILLUSION_AVATAR)) {
                magicDamage += 10;
            }
        }

        return magicDamage;
    }

    public int calcHealing(int skillId) {
        L1Skills l1skills = SkillsTable.getInstance().getTemplate(skillId);
        int dice = l1skills.getDamageDice();
        int value = l1skills.getDamageValue();
        int magicDamage = 0;

        int magicBonus = getMagicBonus();
        if (magicBonus > 10) {
            magicBonus = 10;
        }


        if((skillId == NATURES_BLESSING) && (_pc.getInt() > 25))
        {
            value += _pc.getInt() - 25;
            if(value > 13)
            {
                value = 13;
            }
        }

        int diceCount = value + magicBonus;
        for (int i = 0; i < diceCount; i++) {
            magicDamage += (Random.nextInt(dice) + 1);
        }

        double alignmentRevision = 1.0;
        if (getLawful() > 0) {
            alignmentRevision += (getLawful() / 32768.0);
        }

        magicDamage *= alignmentRevision;

        magicDamage = (magicDamage * getLeverage()) / 10;

        return magicDamage;
    }

    private int calcMrDefense(int dmg) {
        int mr = getTargetMr();

        double mrFloor = 0;
        if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
            if (mr <= 100) {
                mrFloor = Math.floor((mr - _pc.getOriginalMagicHit()) / 2);
            }
            else if (mr >= 100) {
                mrFloor = Math.floor((mr - _pc.getOriginalMagicHit()) / 10);
            }
            double mrCoefficient = 0;
            if (mr <= 100) {
                mrCoefficient = 1 - 0.01 * mrFloor;
            }
            else if (mr >= 100) {
                mrCoefficient = 0.6 - 0.01 * mrFloor;
            }
            dmg *= mrCoefficient;
        }
        else if ((_calcType == NPC_PC) || (_calcType == NPC_NPC)) {
            int rnd = Random.nextInt(100) + 1;
            if (mr >= rnd) {
                dmg /= 2;
            }
        }

        return dmg;
    }

    private double calcAttrResistance(int attr) {
        int resist = 0;
        if ((_calcType == PC_PC) || (_calcType == NPC_PC)) {
            if (attr == L1Skills.ATTR_EARTH) {
                resist = _targetPc.getEarth();
            }
            else if (attr == L1Skills.ATTR_FIRE) {
                resist = _targetPc.getFire();
            }
            else if (attr == L1Skills.ATTR_WATER) {
                resist = _targetPc.getWater();
            }
            else if (attr == L1Skills.ATTR_WIND) {
                resist = _targetPc.getWind();
            }
        }
        else if ((_calcType == PC_NPC) || (_calcType == NPC_NPC)) {}

        int resistFloor = (int) (0.32 * Math.abs(resist));
        if (resist >= 0) {
            resistFloor *= 1;
        }
        else {
            resistFloor *= -1;
        }

        double attrDeffence = resistFloor / 32.0;

        return attrDeffence;
    }



    public void commit(int damage, int drainMana) {
        if ((_calcType == PC_PC) || (_calcType == NPC_PC)) {
            commitPc(damage, drainMana);
        }
        else if ((_calcType == PC_NPC) || (_calcType == NPC_NPC)) {
            commitNpc(damage, drainMana);
        }

        if (!Config.ALT_ATKMSG) {
            return;
        }
        if (Config.ALT_ATKMSG) {
            if (((_calcType == PC_PC) || (_calcType == PC_NPC)) && !_pc.isGm()) {
                return;
            }
            if ((_calcType == NPC_PC) && !_targetPc.isGm()) {
                return;
            }
        }

        String msg0 = "";
        String msg1 = " Ã©â‚¬ Ã¦Ë†ï¿½ ";
        String msg2 = "";
        String msg3 = "";
        String msg4 = "";

        if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
            msg0 = "Ã©Â­â€�Ã¦â€�Â» Ã¥Â°ï¿½";
        }
        else if (_calcType == NPC_PC) {
            msg0 = _npc.getName() + "(Ã©Â­â€�Ã¦â€�Â»)Ã¯Â¼Å¡";
        }

        if ((_calcType == NPC_PC) || (_calcType == PC_PC)) {
            msg4 = _targetPc.getName();
            msg2 = "Ã¯Â¼Å’Ã¥â€°Â©Ã©Â¤Ëœ " + _targetPc.getCurrentHp();
        }
        else if (_calcType == PC_NPC) {
            msg4 = _targetNpc.getName();
            msg2 = "Ã¯Â¼Å’Ã¥â€°Â©Ã©Â¤Ëœ " + _targetNpc.getCurrentHp();
        }

        msg3 = damage  + " Ã¥â€šÂ·Ã¥Â®Â³";

        if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
            _pc.sendPackets(new S_ServerMessage(166, msg0, msg1, msg2, msg3, msg4));
        }
        else if ((_calcType == NPC_PC)) {
            _targetPc.sendPackets(new S_ServerMessage(166, msg0, null, msg2, msg3, null));
        }
    }

    private void commitPc(int damage, int drainMana) {
        if (_calcType == PC_PC) {
            if ((drainMana > 0) && (_targetPc.getCurrentMp() > 0)) {
                if (drainMana > _targetPc.getCurrentMp()) {
                    drainMana = _targetPc.getCurrentMp();
                }
                int newMp = _pc.getCurrentMp() + drainMana;
                _pc.setCurrentMp(newMp);
            }
            _targetPc.receiveManaDamage(_pc, drainMana);
            _targetPc.receiveDamage(_pc, damage, true);
        }
        else if (_calcType == NPC_PC) {
            _targetPc.receiveDamage(_npc, damage, true);
        }
    }

    private void commitNpc(int damage, int drainMana) {
        if (_calcType == PC_NPC) {
            if (drainMana > 0) {
                int drainValue = _targetNpc.drainMana(drainMana);
                int newMp = _pc.getCurrentMp() + drainValue;
                _pc.setCurrentMp(newMp);
            }
            _targetNpc.ReceiveManaDamage(_pc, drainMana);
            _targetNpc.receiveDamage(_pc, damage);
        }
        else if (_calcType == NPC_NPC) {
            _targetNpc.receiveDamage(_npc, damage);
        }
    }
}