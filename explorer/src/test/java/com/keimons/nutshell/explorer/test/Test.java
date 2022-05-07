package com.keimons.nutshell.explorer.test;

import java.util.*;

public class Test {

	@org.junit.jupiter.api.Test
	public void test() {
		List<List<String>> items = new LinkedList<>();
		for (int i = 0; i < 10; i++) {
			List<String> list = new LinkedList<>();
			for (int j = 0; j < 200; j++) {
				list.add(Character.toString((char) 'A' + i));
			}
			items.add(list);
		}

		int count = 8;

		// 分为两组，每次都在先A组拿，再B组拿
		List<List<String>> teamA = new LinkedList<>();
		List<List<String>> teamB = new LinkedList<>();

		teamA.addAll(items);

		Map<String, Integer> maps = new HashMap<>();

		for (int i = 0; i < 2000 / count; i++) {
			List<String> result = new ArrayList<>(count);
			// 打乱两组排序
			Collections.shuffle(teamA);
			Collections.shuffle(teamB);

			List<List<String>> tmpTeamA = new LinkedList<>();
			List<List<String>> tmpTeamB = new LinkedList<>();

			for (List<String> strings : teamA) {
				if (result.size() < count) {
					result.add(strings.remove(0));
				}
			}
			for (List<String> strings : teamB) {
				if (result.size() < count) {
					result.add(strings.remove(0));
				}
			}
			int tmpCount = teamA.size() > 0 ? teamA.get(0).size() : teamB.get(0).size();
			for (List<String> strings : teamA) {
				if (strings.size() == tmpCount) {
					tmpTeamA.add(strings);
				} else {
					tmpTeamB.add(strings);
				}
			}
			for (List<String> strings : teamB) {
				if (strings.size() == tmpCount) {
					tmpTeamA.add(strings);
				} else {
					tmpTeamB.add(strings);
				}
			}
			int tmpA = tmpTeamA.size() > 0 ? tmpTeamA.get(0).size() : -1;
			int tmpB = tmpTeamB.size() > 0 ? tmpTeamB.get(0).size() : -1;
			if (tmpA > tmpB) {
				teamA = tmpTeamA;
				teamB = tmpTeamB;
			} else {
				teamA = tmpTeamB;
				teamB = tmpTeamA;
			}
			for (String s : result) {
				maps.put(s, maps.getOrDefault(s, 0) + 1);
			}
			Collections.shuffle(result);
			System.out.println(Arrays.toString(result.toArray()));
		}
		System.out.println("total: " + maps);
	}
}
