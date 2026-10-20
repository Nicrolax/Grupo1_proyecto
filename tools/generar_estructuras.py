"""
Genera todas las plantillas de estructuras (.nbt, Minecraft 1.16.5) y las tablas de botín:

  pulperia/<bioma>.nbt   una pulpería distinta para cada bioma (ver estructuras_pulperias.py);
                         las mismas se usan al lado de cada aldea
  guarida.nbt            bar clandestino subterráneo (estructuras_guarida.py)
  estancia.nbt           Estancia del Caudillo, única en el mundo (estructuras_estancia.py)
  paso_animas.nbt        el Paso de las Ánimas de día, y salamanca.nbt, de noche (estructuras_paso.py)

Uso: python3 tools/generar_estructuras.py   (requiere: pip install nbtlib)
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from estructuras_base import path, json
from estructuras_pulperias import ESTILOS
from estructuras_guarida import guarida
from estructuras_estancia import estancia
from estructuras_paso import ruina, salamanca


def loot(name, pools):
    with open(path("loot_tables", "chests", name + ".json"), "w") as f:
        json.dump({"type": "minecraft:chest", "pools": pools}, f, indent=2)


if __name__ == "__main__":
    # plantillas viejas que ya no se usan
    for viejo in ("pulperia.nbt", os.path.join("aldea", "pulperia.nbt")):
        p = path("structures", viejo)
        if os.path.exists(p):
            os.remove(p)
    for nombre, fabrica in ESTILOS.items():
        fabrica().save("pulperia", nombre + ".nbt")
    guarida().save("guarida.nbt")
    estancia().save("estancia.nbt")
    ruina().save("paso_animas.nbt")
    salamanca().save("salamanca.nbt")
    loot("guarida", [
        {"rolls": {"min": 3, "max": 6}, "entries": [
            {"type": "minecraft:item", "name": "minecraft:gold_ingot", "weight": 10,
             "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 4}}]},
            {"type": "minecraft:item", "name": "minecraft:emerald", "weight": 8,
             "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 5}}]},
            {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 3},
            {"type": "minecraft:item", "name": "trucoya:ficha_clandestina", "weight": 6,
             "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 3}}]},
            {"type": "minecraft:item", "name": "trucoya:disco_candombe", "weight": 2},
            {"type": "minecraft:item", "name": "trucoya:grapamiel", "weight": 5},
            {"type": "minecraft:item", "name": "minecraft:gold_nugget", "weight": 12,
             "functions": [{"function": "minecraft:set_count", "count": {"min": 3, "max": 12}}]}]}])
    loot("estancia", [
        {"rolls": {"min": 4, "max": 8}, "entries": [
            {"type": "minecraft:item", "name": "minecraft:gold_ingot", "weight": 8,
             "functions": [{"function": "minecraft:set_count", "count": {"min": 2, "max": 6}}]},
            {"type": "minecraft:item", "name": "minecraft:emerald", "weight": 8,
             "functions": [{"function": "minecraft:set_count", "count": {"min": 2, "max": 8}}]},
            {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 4,
             "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 2}}]},
            {"type": "minecraft:item", "name": "minecraft:saddle", "weight": 4},
            {"type": "minecraft:item", "name": "minecraft:leather", "weight": 10,
             "functions": [{"function": "minecraft:set_count", "count": {"min": 2, "max": 7}}]},
            {"type": "minecraft:item", "name": "trucoya:ficha_clandestina", "weight": 5},
            {"type": "minecraft:item", "name": "trucoya:disco_vals", "weight": 2},
            {"type": "minecraft:item", "name": "trucoya:disco_milonga", "weight": 2},
            {"type": "minecraft:item", "name": "trucoya:yerba_mate", "weight": 8,
             "functions": [{"function": "minecraft:set_count", "count": {"min": 2, "max": 6}}]},
            {"type": "minecraft:item", "name": "trucoya:vino_tinto", "weight": 6}]}])
    print("Estructuras generadas:", len(ESTILOS), "pulperías, guarida y estancia.")
