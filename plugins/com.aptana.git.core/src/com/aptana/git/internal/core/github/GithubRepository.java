/**
 * Aptana Studio
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.git.internal.core.github;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.json.simple.JSONObject;

import com.aptana.git.core.GitPlugin;
import com.aptana.git.core.github.IGithubManager;
import com.aptana.git.core.github.IGithubRepository;
import com.aptana.git.core.model.GitRepository;

/**
 * @author cwilliams
 */
public class GithubRepository implements IGithubRepository
{

	/**
	 * Keys used in JSON describing the repository.
	 */
	private static final String PARENT = "parent"; //$NON-NLS-1$
	private static final String LOGIN = "login"; //$NON-NLS-1$
	private static final String OWNER = "owner"; //$NON-NLS-1$
	private static final String DEFAULT_BRANCH = "default_branch"; //$NON-NLS-1$
	private static final String FORK = "fork"; //$NON-NLS-1$
	private static final String PRIVATE = "private"; //$NON-NLS-1$
	private static final String NAME = "name"; //$NON-NLS-1$
	private static final String ID = "id"; //$NON-NLS-1$
	private static final String ATTR_SSH_URL = "ssh_url"; //$NON-NLS-1$

	private JSONObject json;

	public GithubRepository(JSONObject repo)
	{
		this.json = repo;
	}

	public int getID()
	{
		return (Integer) json.get(ID);
	}

	public String getName()
	{
		return (String) json.get(NAME);
	}

	public boolean isPrivate()
	{
		return (Boolean) json.get(PRIVATE);
	}

	public boolean isFork()
	{
		return (Boolean) json.get(FORK);
	}

	public String getSSHURL()
	{
		return (String) json.get(ATTR_SSH_URL);
	}

	public String getDefaultBranch()
	{
		return (String) json.get(DEFAULT_BRANCH);
	}

	public String getOwner()
	{
		JSONObject owner = (JSONObject) json.get(OWNER);
		return (String) owner.get(LOGIN);
	}

	public IGithubRepository getParent() throws CoreException
	{
		if (!isFork())
		{
			return null;
		}

		if (!json.containsKey(PARENT))
		{
			getDetailedJSON();
		}
		// TODO Keep a cache of the repos inside the manager or something?
		return new GithubRepository((JSONObject) json.get(PARENT));
	}

	protected void getDetailedJSON() throws CoreException
	{
		this.json = (JSONObject) getAPI().get(getAPIURL());
	}

	/**
	 * The base URL for operations on this repo in Github's API
	 * 
	 * @return
	 */
	protected String getAPIURL()
	{
		return "repos/" + getOwner() + "/" + getName(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected GithubAPI getAPI()
	{
		return new GithubAPI(getGithubManager().getUser());
	}

	@SuppressWarnings("unchecked")
	public IStatus createPullRequest(String title, String body, GitRepository repo)
	{
		String branch = repo.currentBranch();

		// push current branch to origin first!
		IStatus pushStatus = repo.push(GitRepository.ORIGIN, branch);
		if (!pushStatus.isOK())
		{
			return pushStatus;
		}

		try
		{
			IGithubRepository parent = getParent();
			JSONObject prObject = new JSONObject();
			prObject.put("title", title); //$NON-NLS-1$
			prObject.put("body", body); //$NON-NLS-1$
			// TODO Allow user to choose branch on the fork to use as contents for PR?
			prObject.put("head", getGithubManager().getUser().getUsername() + ':' + branch); //$NON-NLS-1$
			// FIXME Allow user to choose the branch from parent to merge against. Default to the parent's default
			// branch
			prObject.put("base", parent.getDefaultBranch()); //$NON-NLS-1$

			// TODO Do something with the response?
			getAPI().post(((GithubRepository) parent).getAPIURL() + "/pulls", prObject.toJSONString()); //$NON-NLS-1$
		}
		catch (CoreException e)
		{
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

	protected IGithubManager getGithubManager()
	{
		return GitPlugin.getDefault().getGithubManager();
	}
}
