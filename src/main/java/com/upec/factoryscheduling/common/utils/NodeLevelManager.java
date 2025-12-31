package com.upec.factoryscheduling.common.utils;

import com.upec.factoryscheduling.aps.entity.Procedure;

import java.util.*;

public class NodeLevelManager {

    /**
     * 计算节点的层级（BFS广度优先算法）
     * 从根节点开始，逐层计算每个节点的层级
     *
     * @param root 根节点
     */
    public static void calculateLevels(Procedure root) {
        if (root == null) {
            return;
        }
        // 使用队列进行BFS遍历
        Queue<Procedure> queue = new LinkedList<>();
        Map<Procedure, Integer> visited = new HashMap<>();
        // 初始化根节点
        root.setLevel(1);
        root.setIndex(1);
        queue.offer(root);
        visited.put(root, 0);
        while (!queue.isEmpty()) {
            Procedure current = queue.poll();
            int currentLevel = current.getLevel();
            // 处理所有下一个节点
            if (current.getNextProcedure() != null) {
                for (Procedure nextProcedure : current.getNextProcedure()) {
                    // 如果节点尚未访问，或者新层级大于当前层级，则更新
                    int newLevel = currentLevel + 1;
                    if (!visited.containsKey(nextProcedure) || newLevel > visited.get(nextProcedure)) {
                        nextProcedure.setLevel(newLevel);
                        nextProcedure.setIndex(newLevel);
                        visited.put(nextProcedure, newLevel);
                        queue.offer(nextProcedure);
                    }
                }
            }
        }

    }

    /**
     * 计算节点的层级（DFS深度优先算法）
     * 递归计算每个节点的层级
     */
    public static void calculateLevelsDFS(Procedure procedure) {
        if (procedure == null) {
            return;
        }
        // 如果节点没有层级，假设为根节点（层级0）
        if (procedure.getLevel() == null) {
            procedure.setLevel(1);
            procedure.setIndex(1);
        }
        // 递归计算下一个节点的层级
        if (procedure.getNextProcedure() != null) {
            for (Procedure nextProcedure : procedure.getNextProcedure()) {
                int newLevel = procedure.getLevel() + 1;
                // 如果下一个节点还没有层级，或者新层级更大，则更新
                if (nextProcedure.getLevel() == null || newLevel > nextProcedure.getLevel()) {
                    nextProcedure.setLevel(newLevel);
                    nextProcedure.setIndex(newLevel);
                    calculateLevelsDFS(nextProcedure);
                }
            }
        }
    }

    /**
     * 获取最大层级（图的最大深度）
     */
    public static int getMaxLevel(Procedure root) {
        if (root == null) {
            return -1;
        }

        // 确保层级已计算
        if (root.getLevel() == null) {
            calculateLevels(root);
        }

        int maxLevel = 0;
        Queue<Procedure> queue = new LinkedList<>();
        Set<Procedure> visited = new HashSet<>();

        queue.offer(root);
        visited.add(root);

        while (!queue.isEmpty()) {
            Procedure current = queue.poll();

            // 更新最大层级
            if (current.getLevel() != null && current.getLevel() > maxLevel) {
                maxLevel = current.getLevel();
            }

            if (current.getNextProcedure() != null) {
                for (Procedure nextProcedure : current.getNextProcedure()) {
                    if (!visited.contains(nextProcedure)) {
                        visited.add(nextProcedure);
                        queue.offer(nextProcedure);
                    }
                }
            }
        }

        return maxLevel;
    }

    /**
     * 获取指定层级的节点列表
     */
    public static List<Procedure> getProcedureByLevel(Procedure root, int targetLevel) {
        List<Procedure> result = new ArrayList<>();

        if (root == null || targetLevel < 0) {
            return result;
        }

        // 确保层级已计算
        if (root.getLevel() == null) {
            calculateLevels(root);
        }

        Queue<Procedure> queue = new LinkedList<>();
        Set<Procedure> visited = new HashSet<>();

        queue.offer(root);
        visited.add(root);

        while (!queue.isEmpty()) {
            Procedure current = queue.poll();

            // 如果当前节点层级等于目标层级，添加到结果
            if (current.getLevel() != null && current.getLevel() == targetLevel) {
                result.add(current);
            }

            if (current.getNextProcedure() != null) {
                for (Procedure nextProcedure : current.getNextProcedure()) {
                    if (!visited.contains(nextProcedure)) {
                        visited.add(nextProcedure);
                        queue.offer(nextProcedure);
                    }
                }
            }
        }

        // 按索引排序
        result.sort(Comparator.comparingInt(Procedure::getIndex));

        return result;
    }

    /**
     * 按层级分组节点
     */
    public static Map<Integer, List<Procedure>> groupProcedureByLevel(Procedure root) {
        Map<Integer, List<Procedure>> levelMap = new TreeMap<>();
        if (root == null) {
            return levelMap;
        }
        // 确保层级已计算
        if (root.getLevel() == null) {
            calculateLevels(root);
        }
        Queue<Procedure> queue = new LinkedList<>();
        Set<Procedure> visited = new HashSet<>();
        queue.offer(root);
        visited.add(root);
        while (!queue.isEmpty()) {
            Procedure current = queue.poll();
            // 将节点添加到对应的层级列表
            if (current.getLevel() != null) {
                int level = current.getLevel();
                levelMap.computeIfAbsent(level, k -> new ArrayList<>()).add(current);
            }
            if (current.getNextProcedure() != null) {
                for (Procedure nextProcedure : current.getNextProcedure()) {
                    if (!visited.contains(nextProcedure)) {
                        visited.add(nextProcedure);
                        queue.offer(nextProcedure);
                    }
                }
            }
        }
        // 每个层级的节点按索引排序
        for (List<Procedure> procedures : levelMap.values()) {
            procedures.sort(Comparator.comparingInt(Procedure::getIndex));
        }
        return levelMap;
    }

    /**
     * 查找节点（按索引）
     */
    public static Procedure findProcedureByIndex(Procedure root, int targetIndex) {
        if (root == null) {
            return null;
        }
        Queue<Procedure> queue = new LinkedList<>();
        Set<Procedure> visited = new HashSet<>();
        queue.offer(root);
        visited.add(root);
        while (!queue.isEmpty()) {
            Procedure current = queue.poll();
            if (current.getIndex() == targetIndex) {
                return current;
            }
            if (current.getNextProcedure() != null) {
                for (Procedure nextProcedure : current.getNextProcedure()) {
                    if (!visited.contains(nextProcedure)) {
                        visited.add(nextProcedure);
                        queue.offer(nextProcedure);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取从根节点到目标节点的路径
     */
    public static List<Procedure> getPathToProcedure(Procedure root, int targetIndex) {
        List<Procedure> path = new ArrayList<>();

        if (root == null) {
            return path;
        }
        // 查找目标节点
        Procedure target = findProcedureByIndex(root, targetIndex);
        if (target == null) {
            return path;
        }
        // 使用回溯法查找路径
        Map<Procedure, Procedure> parentMap = new HashMap<>();
        Queue<Procedure> queue = new LinkedList<>();
        Set<Procedure> visited = new HashSet<>();
        queue.offer(root);
        visited.add(root);
        parentMap.put(root, null);
        while (!queue.isEmpty()) {
            Procedure current = queue.poll();
            // 如果找到目标节点，回溯构建路径
            if (current.equals(target)) {
                Procedure Procedure = current;
                while (Procedure != null) {
                    path.add(0, Procedure); // 添加到路径开头
                    Procedure = parentMap.get(Procedure);
                }
                break;
            }

            if (current.getNextProcedure() != null) {
                for (Procedure nextProcedure : current.getNextProcedure()) {
                    if (!visited.contains(nextProcedure)) {
                        visited.add(nextProcedure);
                        parentMap.put(nextProcedure, current);
                        queue.offer(nextProcedure);
                    }
                }
            }
        }

        return path;
    }

    /**
     * 打印层级结构
     */
    public static void printHierarchy(Procedure root) {
        if (root == null) {
            System.out.println("空图");
            return;
        }

        Map<Integer, List<Procedure>> levelMap = groupProcedureByLevel(root);

        System.out.println("层级结构:");
        for (Map.Entry<Integer, List<Procedure>> entry : levelMap.entrySet()) {
            System.out.print("层级 " + entry.getKey() + ": ");
            for (Procedure Procedure : entry.getValue()) {
                System.out.print("[" + Procedure.getIndex() + "] ");
            }
            System.out.println();
        }
    }

    /**
     * 重置所有节点的层级
     */
    public static void resetLevels(Procedure root) {
        if (root == null) {
            return;
        }
        Queue<Procedure> queue = new LinkedList<>();
        Set<Procedure> visited = new HashSet<>();
        queue.offer(root);
        visited.add(root);
        while (!queue.isEmpty()) {
            Procedure current = queue.poll();
            current.setLevel(null);
            if (current.getNextProcedure() != null) {
                for (Procedure nextProcedure : current.getNextProcedure()) {
                    if (!visited.contains(nextProcedure)) {
                        visited.add(nextProcedure);
                        queue.offer(nextProcedure);
                    }
                }
            }
        }
    }
}
